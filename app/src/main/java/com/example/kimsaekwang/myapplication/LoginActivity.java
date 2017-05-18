package com.example.kimsaekwang.myapplication;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.IOException;

/**
 * Created by user on 2017-05-17.
 */

public class LoginActivity extends AppCompatActivity {

    private static final int PICK_FROM_CAMERA = 0; //카메라촬영
    private static final int PICK_FROM_ALBUM = 1; //앨범에서

    private int id_view;

    private ImageView imageView;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button btn_photo = (Button) findViewById(R.id.btn_photo);
        imageView = (ImageView) findViewById(R.id.plantphoto);

        btn_photo.setOnClickListener(mClickListener);
    }

    public void doTakePhotoAction() //카메라 촬영 후 이미지 가져옴
    {

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString());

        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 0);
        intent.putExtra("aspectY", 0);
        intent.putExtra("outputX", 200);
        intent.putExtra("outputY", 150);

        try {
            intent.putExtra("return-data", true);
            startActivityForResult(intent, PICK_FROM_CAMERA);
        } catch (ActivityNotFoundException e) {
            // Do nothing for now
        }

    }

    public void doTakeAlbumAction() //앨범에서 가져옴
    {
        Intent intent = new Intent(Intent.ACTION_PICK);

        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        intent.setAction(Intent.ACTION_GET_CONTENT);

        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 0);
        intent.putExtra("aspectY", 0);
        intent.putExtra("outputX", 200);
        intent.putExtra("outputY", 150);
        try {
            intent.putExtra("return-data", true);
            startActivityForResult(intent, PICK_FROM_ALBUM);
        } catch (ActivityNotFoundException e) {
            // Do nothing for now
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PICK_FROM_CAMERA) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap photo = extras.getParcelable("data");
                imageView.setImageBitmap(photo);
            }
        } else if (requestCode == PICK_FROM_ALBUM) {
            Uri photoURI = data.getData();

            Bitmap photo = null;
            try {
                photo = MediaStore.Images.Media.getBitmap(getContentResolver(), photoURI);
            } catch (IOException e) {
                e.printStackTrace();
            }
            imageView.setImageBitmap(photo);
        }
    }

    Button.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            id_view = v.getId();
            if (v.getId() == R.id.btn_photo) {
                DialogInterface.OnClickListener cameraListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        doTakePhotoAction();
                    }
                };
                DialogInterface.OnClickListener albumListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        doTakeAlbumAction();
                    }
                };
                DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                };

                new AlertDialog.Builder(v.getContext()) //Context
                        .setTitle("업로드할 이미지 선택")
                        .setPositiveButton("사진촬영", cameraListener)
                        .setNeutralButton("앨범선택", albumListener)
                        .setNegativeButton("취소", cancelListener)
                        .show();
            }
        }

    };
}



