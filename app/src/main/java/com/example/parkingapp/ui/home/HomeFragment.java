package com.example.parkingapp.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.daimajia.slider.library.Animations.DescriptionAnimation;
import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.TextSliderView;
import com.daimajia.slider.library.Tricks.ViewPagerEx;
import com.example.parkingapp.HomePage;
import com.example.parkingapp.LoginActivity;
import com.example.parkingapp.R;
import com.example.parkingapp.SingleTask;
import com.example.parkingapp.adapter.ModuleAdapter;
import com.example.parkingapp.model.Book;
import com.example.parkingapp.model.Feedback;
import com.example.parkingapp.model.Module;
import com.example.parkingapp.model.Profile;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class HomeFragment extends Fragment implements BaseSliderView.OnSliderClickListener, ViewPagerEx.OnPageChangeListener {
    private RecyclerView moduleRecyclerView;
    private List<Module> moduleList;
    private SliderLayout sliderLayout;
    private HashMap<String, Integer> Hash_file_maps;
    private Button logoutbutton;
    private Switch switchstatus;
    private Profile profile;
    private SharedPreferences sharedPreferences;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        sharedPreferences = getActivity().getSharedPreferences("session", Context.MODE_PRIVATE);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //initialize all views
        initViews(view);
        //setlayout in recyclerview either grid or list
        moduleRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
        //call your method where module list is define
        myModules();
        //give modules list to the custom moduleadapter class
        ModuleAdapter moduleAdapter = new ModuleAdapter(moduleList);
        //call custom method from moduleadater class
        moduleAdapter.setOnMyModuleClickListener(new ModuleAdapter.MyModuleClickListener() {
            @Override
            public void onModuleClick(View view, int position) {
                NavController navController = Navigation.findNavController(view);
                //get Module object from module list
                Module module = moduleList.get(position);
                if (module.getName().equalsIgnoreCase(getResources().getString(R.string.menu_book_parking))) {
                    //go to book parking page
                    Toast.makeText(getActivity(), "Booking", Toast.LENGTH_SHORT).show();
                    String owneruid = sharedPreferences.getString("status", null);
                    if (owneruid == null) {
                        navController.navigate(R.id.action_nav_home_to_nav_book_parking);
                    } else {
                        checkBookingStatus(owneruid);
                    }
                } else if (module.getName().equalsIgnoreCase(getResources().getString(R.string.menu_profile))) {
                    //go to profile page
                    navController.navigate(R.id.action_nav_home_to_nav_profile);
                    Toast.makeText(getActivity(), "Profile", Toast.LENGTH_SHORT).show();
                } else if (module.getName().equalsIgnoreCase(getResources().getString(R.string.menu_parking_history))) {
                    //go to parking history page
                    Toast.makeText(getActivity(), "Parking", Toast.LENGTH_SHORT).show();
                    navController.navigate(R.id.action_nav_home_to_nav_parking_history);
                } else if (module.getName().equalsIgnoreCase(getResources().getString(R.string.menu_add_parking))) {
                    //go to add parking page
                    navController.navigate(R.id.action_nav_home_to_nav_add_parking);
                    Toast.makeText(getActivity(), "Add Parking", Toast.LENGTH_SHORT).show();
                }

            }
        });
        //give module adapter object to recyclerview
        moduleRecyclerView.setAdapter(moduleAdapter);
        //logout code here
        logoutbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });
    }


    private Book currentBooking;
    private Profile ownerProfileDetail;
    private float totalamount;

    private void checkBookingStatus(final String owneruid) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.payment_alert, null, false);
        //alert dialog
        AlertDialog.Builder a = new AlertDialog.Builder(getActivity());
        final AlertDialog alertDialog = a.create();
        alertDialog.setView(view);
        alertDialog.setCancelable(false);
        alertDialog.show();
        //get current booking detail
        getBookingDetail(owneruid);
        //initialize object here
        final TextView tstarttime = view.findViewById(R.id.booking_starttime);
        final TextView tendtime = view.findViewById(R.id.booking_endtime);
        final TextView ttotalprice = view.findViewById(R.id.booking_total_price);

        final ImageView closebutton = view.findViewById(R.id.close_icon);
        final Button stopbutton = view.findViewById(R.id.booking_button_stop);
        final Button feedbutton = view.findViewById(R.id.booking_button_feedback);
        feedbutton.setVisibility(View.GONE);
        final Button paybutton = view.findViewById(R.id.booking_button_pay);
        paybutton.setVisibility(View.GONE);
        getOwnerProfileData(owneruid);
        //perform buttons event here
        stopbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentBooking != null) {
                    int endhour = Calendar.getInstance().get(Calendar.HOUR);
                    int endminute = Calendar.getInstance().get(Calendar.MINUTE);

                    int totalendinminute = endhour * 60 + endminute;

                    tstarttime.setText(currentBooking.getHour() + ":" + currentBooking.getMinute());
                    tendtime.setText(endhour + ":" + endminute);

                    //start time - end time
                    int totalstartinminute = Integer.parseInt(currentBooking.getHour()) * 60 + Integer.parseInt(currentBooking.getMinute());

                    int totalduration = totalendinminute - totalstartinminute;

                    float perminutecharge = Float.parseFloat(ownerProfileDetail.getChargeperhour()) / 60;
                    Log.e("error per minute ",perminutecharge+"");

                    totalamount = totalduration * perminutecharge;

                    ttotalprice.setText(String.valueOf(totalamount));

                    feedbutton.setVisibility(View.VISIBLE);
                    stopbutton.setVisibility(View.GONE);
                }
            }
        });

        feedbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //open feedback form after given feedback enable paybutton
                View v = getActivity().getLayoutInflater().inflate(R.layout.feedback_alert, null, false);
                //alert dialog of feedback here
                AlertDialog.Builder a = new AlertDialog.Builder(getActivity());
                final AlertDialog alertDialog = a.create();
                alertDialog.setView(v);
                alertDialog.setCancelable(false);
                alertDialog.show();
                //initialize all view here
                v.findViewById(R.id.feedback_close_icon).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        alertDialog.dismiss();
                    }
                });

                TextView townernameandmobile = v.findViewById(R.id.ownernameandmobile);
                TextView towneraddress = v.findViewById(R.id.owneraddress);
                final EditText comments = v.findViewById(R.id.commentshere);
                final TextView ratevalue = v.findViewById(R.id.ratevalue);
                final RatingBar ratingBar = v.findViewById(R.id.feedback_rating);

                Button feedsubmit = v.findViewById(R.id.feedback_submit);

                //set details in UI
                townernameandmobile.setText("Mr./Ms. " + ownerProfileDetail.getName() + "\t" + "Mob. " + ownerProfileDetail.getMobile());
                towneraddress.setText(ownerProfileDetail.getAddress());

                feedsubmit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String message = comments.getText().toString();
                        if (TextUtils.isEmpty(message)) {
                            Toast.makeText(getActivity(), "Please Enter Comments", Toast.LENGTH_SHORT).show();
                            comments.requestFocus();
                        } else if (ratevalue.getText().toString().equalsIgnoreCase("0.0")) {
                            Toast.makeText(getActivity(), "Please Give Star", Toast.LENGTH_SHORT).show();
                            ratingBar.requestFocus();
                        } else {
                            Feedback feedback = new Feedback();
                            feedback.setMessage(message);
                            feedback.setRate(Float.parseFloat(ratevalue.getText().toString()));
                            feedback.setOwneruid(ownerProfileDetail.getUid());
                            feedback.setRenteruid(profile.getUid());
                            //store feedback in feedback section
                            ((SingleTask) getActivity().getApplication()).getFeedbackDatabaseReference().child(ownerProfileDetail.getUid()).push().setValue(feedback).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(getActivity(), "Successfully Feedback Given", Toast.LENGTH_SHORT).show();
                                        paybutton.setVisibility(View.VISIBLE);
                                        feedbutton.setVisibility(View.GONE);
                                        alertDialog.dismiss();
                                    }
                                }
                            });
                        }

                    }
                });

                ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
                    @Override
                    public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                        ratevalue.setText(String.valueOf(v));
                    }
                });

            }
        });

        paybutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //update booking status in parking
                //remove from shareprefrences
                //dismiss the alert
                ((SingleTask) getActivity().getApplication()).getParkingDatabaseReference().child(ownerProfileDetail.getCityname()).child(ownerProfileDetail.getUid() + "/bookstatus").setValue(true).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            String key = sharedPreferences.getString("pushkey", null);
                            if (key != null) {
                                ((SingleTask) getActivity().getApplication()).getBookingDatabaseReference().child(profile.getUid()).child(key + "/totalprice").setValue(String.valueOf(totalamount)).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(getActivity(), "Successfully Payment", Toast.LENGTH_SHORT).show();
                                            sharedPreferences.edit().remove("status").commit();
                                            sharedPreferences.edit().remove("pushkey").commit();
                                            alertDialog.dismiss();
                                        }
                                    }
                                });

                            }
                        }
                    }
                });
            }
        });

        closebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });
    }

    private void getBookingDetail(final String owneruid) {
        ((SingleTask) getActivity().getApplication()).getBookingDatabaseReference().child(profile.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Iterator<DataSnapshot> it = snapshot.getChildren().iterator();
                while (it.hasNext()) {
                    DataSnapshot ds = it.next();
                    Log.e("error", ds.getValue() + "");
                    Book book = ds.getValue(Book.class);
                    if (book.getOwneruid().equals(owneruid)) {
                        currentBooking = book;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getOwnerProfileData(String owneruid) {
        ((SingleTask) getActivity().getApplication()).getProfileDatabaseReference().child(owneruid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ownerProfileDetail = snapshot.getValue(Profile.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void logout() {
        ((SingleTask) getActivity().getApplication()).getmGoogleSignInClient().signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    ((SingleTask) getActivity().getApplication()).getFirebaseAuth().signOut();
                    startActivity(new Intent(getActivity(), LoginActivity.class));
                    getActivity().finish();

                }
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        sliderLayout.stopAutoCycle();
    }

    @Override
    public void onStart() {
        super.onStart();
        sliderLayout.startAutoCycle();
    }

    private void myModules() {
        //create first module and property like name and image
        Module m1 = new Module(getResources().getString(R.string.menu_book_parking), R.drawable.booking);
        //create second module and property like name and image
        Module m2 = new Module(getResources().getString(R.string.menu_profile), R.drawable.profile);
        //create third module and property like name and image
        Module m3 = new Module(getResources().getString(R.string.menu_add_parking), R.drawable.add_parking);
        //create fourth module and property like name and image
        Module m4 = new Module(getResources().getString(R.string.menu_parking_history), R.drawable.parking_history);

        //add all module in list
        moduleList = new ArrayList<>();
        moduleList.add(m1);
        moduleList.add(m2);
        moduleList.add(m3);
        moduleList.add(m4);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getProfileData();
    }

    public Profile getProfileData() {

        String currentuid = ((SingleTask) getActivity().getApplication()).getFirebaseAuth().getCurrentUser().getUid();
        if (currentuid != null) {
            ((SingleTask) getActivity().getApplication()).getProfileDatabaseReference().child(currentuid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.e("profiledata", snapshot.toString());
                    profile = snapshot.getValue(Profile.class);
                    Log.e("profiledata", profile + "");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        } else {
            Log.e("profile data", "please try after some time");
        }
        return profile;
    }

    private void initViews(View view) {
        switchstatus = view.findViewById(R.id.parking_switch);
        moduleRecyclerView = view.findViewById(R.id.mymodulerecycler);
        sliderLayout = view.findViewById(R.id.slider);
        Hash_file_maps = new HashMap();
        logoutbutton = view.findViewById(R.id.logoutbutton);

        mySliderImages();

        switchstatus.setChecked(true);
        switchstatus.setText("Parking Is Available");

        switchstatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    switchstatus.setChecked(true);
                    switchstatus.setText("Parking Is Available");
                    parkingBookStatus(true);
                } else {
                    switchstatus.setChecked(false);
                    switchstatus.setText("Parking Is Not Available");
                    parkingBookStatus(false);
                }
            }
        });

    }

    private void parkingBookStatus(boolean status) {
        Log.e("error", profile.getCityname());
        //update parking status here
        ((SingleTask) getActivity().getApplication()).getParkingDatabaseReference().child(profile.getCityname()).child(profile.getUid() + "/bookstatus").setValue(status).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(getActivity(), "Update Status", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void mySliderImages() {
        Hash_file_maps.put("Always check your area around.", R.drawable.park1);
        Hash_file_maps.put("There must not be litter on the ground.", R.drawable.park2);
        Hash_file_maps.put("Keep Parking clean to make it disease free.", R.drawable.park3);
        Hash_file_maps.put("Come, join and pledge together to park.", R.drawable.park4);
        Hash_file_maps.put("always follow the lane.", R.drawable.park5);

        for (String name : Hash_file_maps.keySet()) {

            TextSliderView textSliderView = new TextSliderView(getActivity());
            textSliderView
                    .description(name)
                    .image(Hash_file_maps.get(name))
                    .setScaleType(BaseSliderView.ScaleType.Fit)
                    .setOnSliderClickListener(this);
            textSliderView.bundle(new Bundle());
            textSliderView.getBundle()
                    .putString("extra", name);
            sliderLayout.addSlider(textSliderView);
        }

        sliderLayout.setPresetTransformer(SliderLayout.Transformer.Accordion);
        sliderLayout.setPresetIndicator(SliderLayout.PresetIndicators.Center_Bottom);
        sliderLayout.setCustomAnimation(new DescriptionAnimation());
        sliderLayout.setDuration(3000);
        sliderLayout.addOnPageChangeListener(this);

    }

    @Override
    public void onSliderClick(BaseSliderView slider) {

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
