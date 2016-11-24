package com.vansuita.pickimage;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import static android.app.Activity.RESULT_OK;
import static com.vansuita.pickimage.R.layout.dialog;
import static com.vansuita.pickimage.Util.tempUri;


/**
 * Created by jrvansuita on 01/11/16.
 */

public class PickImageDialog extends DialogFragment {

    public static PickImageDialog newInstance(PickSetup setup) {
        PickImageDialog frag = new PickImageDialog();
        Bundle args = new Bundle();
        args.putSerializable(SETUP_TAG, setup);
        frag.setArguments(args);
        return frag;
    }

    public static PickImageDialog on(AppCompatActivity activity, PickSetup setup) {
        PickImageDialog d = PickImageDialog.newInstance(setup);
        d.show(activity.getSupportFragmentManager(), "dialog");
        return d;
    }

    private static final String SETUP_TAG = "SETUP_TAG";
    private static final int FROM_CAMERA = 1;
    private static final int FROM_GALLERY = 2;

    private CardView cvRoot;
    private TextView tvTitle;
    private TextView tvCamera;
    private TextView tvGallery;
    private TextView tvCancel;

    private IPickResult.IPickResultBitmap bitmapListener;
    private IPickResult.IPickResultUri uriListener;
    private IPickResult.IPickClick clickListener;
    private IPickResult.IPickError errorListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof IPickResult.IPickResultBitmap)
            bitmapListener = (IPickResult.IPickResultBitmap) context;

        if (context instanceof IPickResult.IPickResultUri)
            uriListener = (IPickResult.IPickResultUri) context;


        if (context instanceof IPickResult.IPickClick)
            clickListener = (IPickResult.IPickClick) context;


        if (context instanceof IPickResult.IPickError)
            errorListener = (IPickResult.IPickError) context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        cvRoot = (CardView) inflater.inflate(dialog, null, false);

        bindView();
        setUp();
        bindListeners();

        requestPermissions();

        return cvRoot;
    }

    private PickSetup setup;

    private void setUp() {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        setup = (PickSetup) getArguments().getSerializable(SETUP_TAG);

        cvRoot.setCardBackgroundColor(setup.getBackgroundColor());
        tvTitle.setTextColor(setup.getTitleColor());
        tvCamera.setTextColor(setup.getOptionsColor());
        tvGallery.setTextColor(setup.getOptionsColor());
        tvCancel.setText(setup.getCancelText());

        tvTitle.setText(setup.getTitle());

        Util.setDimAmount(setup.getDimAmount(), getDialog());
    }

    private void bindView() {
        tvTitle = (TextView) cvRoot.findViewById(R.id.title);
        tvCamera = (TextView) cvRoot.findViewById(R.id.camera);
        tvGallery = (TextView) cvRoot.findViewById(R.id.gallery);
        tvCancel = (TextView) cvRoot.findViewById(R.id.cancel);
    }

    private void bindListeners() {
        tvCancel.setOnClickListener(listener);
        tvCamera.setOnClickListener(listener);
        tvGallery.setOnClickListener(listener);
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.cancel) {
                dismiss();

            } else if (view.getId() == R.id.camera) {
                if (clickListener != null) {
                    clickListener.onCameraClick();
                } else {
                    Util.launchCamera(PickImageDialog.this, FROM_CAMERA);
                }

            } else if (view.getId() == R.id.gallery) {
                if (clickListener != null) {
                    clickListener.onGaleryClick();
                } else {
                    Util.launchGalery(PickImageDialog.this, FROM_GALLERY);
                }

            }
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        dismiss();

        if (resultCode == RESULT_OK)
            try {
                if (bitmapListener != null) {

                    Bitmap bitmap = null;

                    if (requestCode == FROM_CAMERA) {
                        bitmap = Util.decodeUri(tempUri(), getContext(), setup.getImageSize());

                        // Not getting the sample image
                        // bitmap = (Bitmap) data.getExtras().get("data");

                        if (setup.isFlipped())
                            bitmap = Util.flip(bitmap);

                    } else if (requestCode == FROM_GALLERY) {
                        bitmap = Util.decodeUri(data.getData(), getContext(), setup.getImageSize());
                    }

                    bitmapListener.onPickImageResult(bitmap);
                }

                if (uriListener != null) {
                    if (requestCode == FROM_CAMERA) {
                        uriListener.onPickImageResult(tempUri());
                    } else if (requestCode == FROM_GALLERY) {
                        uriListener.onPickImageResult(data.getData());
                    }
                }
            } catch (Exception e) {
                errorListener.onPickError(e);
            }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                dismissAllowingStateLoss();
                break;
            }
        }
    }

    public boolean requestPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            if ((getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                    getActivity().checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                return true;
            }else{
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA}, 1);
                return false;
            }
        }

        return true;
    }

}