package com.example.parkingapp.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.parkingapp.HomePage;
import com.example.parkingapp.R;
import com.example.parkingapp.SingleTask;
import com.example.parkingapp.model.Profile;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

public class ProfileFragment extends Fragment {
    private Profile profile;
    private ImageView timageiconproof;
    private EditText tname, tmobile, taddress, tchargeperhour, tvehiclenumber, tuploadtext, tcityname;
    private String imageurl;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);

        //check that profile is available or not
        profile = ((HomePage) getActivity()).getProfileData();
        if (profile != null) {
            tname.setText(profile.getName());
            tmobile.setText(profile.getMobile());
            tcityname.setText(profile.getCityname());
            taddress.setText(profile.getAddress());
            tchargeperhour.setText(profile.getChargeperhour());
            tvehiclenumber.setText(profile.getVehiclenumber());
            tuploadtext.setText(profile.getIdproofurl());
        }

        timageiconproof.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu pm = new PopupMenu(getActivity(), timageiconproof);
                pm.getMenuInflater().inflate(R.menu.popup, pm.getMenu());
                pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.gallery:
                                openGallery();
                                break;
                            case R.id.camera:
                                openCamera();
                                break;
                        }
                        return true;
                    }
                });
                pm.show();
            }
        });
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, 1001);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 1002);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 1001) {
                //camera handling code here
                cameraHandlingCode(data);
            }

            if (requestCode == 1002) {
                //gallery handling code here
                galleryHandlingCode(data);
            }
        }

    }

    private byte bb[];

    private void cameraHandlingCode(Intent data) {
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        bb = bytes.toByteArray();
        tuploadtext.setText(data.getData().toString());
        //imageurl = Base64.encodeToString(bb, Base64.DEFAULT);
    }

    private void galleryHandlingCode(Intent data) {
        Uri selectedImage = data.getData();
        String[] filePath = {MediaStore.Images.Media.DATA};
        Cursor c = getActivity().getContentResolver().query(selectedImage, filePath, null, null, null);
        c.moveToFirst();
        String picturePath = c.getString(c.getColumnIndex(filePath[0]));
        c.close();
        Bitmap thumbnail = (BitmapFactory.decodeFile(picturePath));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        bb = bytes.toByteArray();
        tuploadtext.setText(data.getData().toString());
        //imageurl = Base64.encodeToString(bb, Base64.DEFAULT);

    }

    private void initViews(View view) {
        tname = view.findViewById(R.id.profile_name);
        tcityname = view.findViewById(R.id.profile_city);
        tmobile = view.findViewById(R.id.profile_mobile);
        taddress = view.findViewById(R.id.profile_address);
        tchargeperhour = view.findViewById(R.id.profile_charge_per_hour);
        tvehiclenumber = view.findViewById(R.id.profile_vehicle_number);
        tuploadtext = view.findViewById(R.id.profile_uploadtext);
        timageiconproof = view.findViewById(R.id.profile_uploadidprooficon);

        Button updateprofile = view.findViewById(R.id.profile_button);
        updateprofile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (valid()) {
                    final String currentuid = ((SingleTask) getActivity().getApplication()).getFirebaseAuth().getCurrentUser().getUid();
                    if (profile != null) {
                        //update
                        profile.setName(name);
                        profile.setMobile(mobile);
                        profile.setAddress(address);
                        profile.setCityname(cityname);
                        profile.setChargeperhour(charge_per_hour);
                        profile.setVehiclenumber(vehicle_number);

                    } else {
                        //insert
                        profile = new Profile();
                        profile.setUid(currentuid);
                        profile.setName(name);
                        profile.setMobile(mobile);
                        profile.setAddress(address);
                        profile.setCityname(cityname);
                        profile.setChargeperhour(charge_per_hour);
                        profile.setVehiclenumber(vehicle_number);
                    }

                    if (profile.getIdproofurl() != null) {
                        profile.setIdproofurl(currentuid + ".jpg");
                        if (bb != null) {
                            uploadData(currentuid);
                        } else {
                            profile.setIdproofurl(currentuid + ".jpg");
                            //get file name and store into realtimedatabase
                            ((SingleTask) getActivity().getApplication()).getProfileDatabaseReference().child(profile.getUid()).setValue(profile).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(getActivity(), "Successfully Update", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    } else {
                        //first upload file in storage then get url
                        uploadData(currentuid);
                    }


                    //after get the url add this url to profile model class


                }
            }
        });
    }

    private void uploadData(final String currentuid) {
        //first upload file in storage then get url
        ((SingleTask) getActivity().getApplication()).getDocumentStorageIdProof().child(currentuid + ".jpg").putBytes(bb).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                if (taskSnapshot.getTask().isSuccessful()) {

                    profile.setIdproofurl(currentuid + ".jpg");
                    //get file name and store into realtimedatabase
                    ((SingleTask) getActivity().getApplication()).getProfileDatabaseReference().child(profile.getUid()).setValue(profile).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(getActivity(), "Successfully Insert", Toast.LENGTH_SHORT).show();
                        }
                    });


                }
            }
        });
    }

    private String name, mobile, address, charge_per_hour, vehicle_number, uploadtext, cityname;

    private boolean valid() {
        name = tname.getText().toString();
        mobile = tmobile.getText().toString();
        cityname = tcityname.getText().toString();
        address = taddress.getText().toString();
        charge_per_hour = tchargeperhour.getText().toString();
        vehicle_number = tvehiclenumber.getText().toString();
        uploadtext = tuploadtext.getText().toString();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(getActivity(), "Please Enter Name", Toast.LENGTH_SHORT).show();
            tname.requestFocus();
            return false;
        } else if (TextUtils.isEmpty(mobile)) {
            Toast.makeText(getActivity(), "Please Enter Mobile", Toast.LENGTH_SHORT).show();
            tmobile.requestFocus();
            return false;
        } else if (TextUtils.isEmpty(cityname)) {
            Toast.makeText(getActivity(), "Please Enter City Name", Toast.LENGTH_SHORT).show();
            tcityname.requestFocus();
            return false;
        } else if (TextUtils.isEmpty(address)) {
            Toast.makeText(getActivity(), "Please Enter Address", Toast.LENGTH_SHORT).show();
            taddress.requestFocus();
            return false;
        } else if (TextUtils.isEmpty(charge_per_hour)) {
            Toast.makeText(getActivity(), "Please Enter Per Hour Charge", Toast.LENGTH_SHORT).show();
            tchargeperhour.requestFocus();
            return false;
        } else if (TextUtils.isEmpty(vehicle_number)) {
            Toast.makeText(getActivity(), "Please Enter Vehicle Number", Toast.LENGTH_SHORT).show();
            tvehiclenumber.requestFocus();
            return false;
        } else if (TextUtils.isEmpty(uploadtext)) {
            Toast.makeText(getActivity(), "Please Upload ID Proof (Aadhar card/ Pan Card/ Voter Card)", Toast.LENGTH_SHORT).show();
            tuploadtext.requestFocus();
            return false;
        } else {
            return true;
        }
    }


}
