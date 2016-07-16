package com.spb.kbv.imagesgal;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;

public class ImageFragment extends Fragment implements View.OnClickListener {
    static final String ARGUMENT_IMAGE = "arg_image";

    Image image;
    TextView textView;
    ImageView addToFavoriteButton;
    OnImageClickEventListener onClickEventListener;

    //constructor for creating Fragment with Image argument
    static ImageFragment newInstance(Image image){
        ImageFragment imageFragment = new ImageFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_IMAGE, image);
        imageFragment.setArguments(arguments);
        return imageFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        image = getArguments().getParcelable(ARGUMENT_IMAGE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image, container, false);
        final TouchImageView imageView = (TouchImageView) view.findViewById(R.id.imageViewPager);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickEventListener.showActionbBar();
            }
        });
        addToFavoriteButton = (ImageView)view.findViewById(R.id.favorite_button);
        if (image.isFavorite()) {
            addToFavoriteButton.setImageResource(R.mipmap.ic_favorite_fill);
        }
        textView = (TextView) view.findViewById(R.id.textView);
        textView.setText(image.getComment());
        final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);
        Glide.with(getContext()).load(image.getUrl()).asBitmap()
                .listener(new RequestListener<String, Bitmap>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<Bitmap> target, boolean isFirstResource) {
                        Toast.makeText(getContext(), "Please check internet connection." + e.toString(), Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        imageView.setImageBitmap(resource);
                        imageView.setZoom(1.0f);
                        addToFavoriteButton.setOnClickListener(ImageFragment.this);
                    }
                });

        onClickEventListener = (OnImageClickEventListener)getActivity();
        return view;
    }

    @Override
    public void onClick(View v) {
        onClickEventListener.onTimerPause();

        //start alerddialog after click on image to enter comment and add image to favorite
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_to_favorites, null);
        dialogBuilder.setView(dialogView);

        final EditText editText = (EditText) dialogView.findViewById(R.id.editText);
        if (image.isFavorite()) {
            editText.setText(image.getComment());
        }

        /*dialogBuilder.setTitle("Add to favorite");*/
        dialogBuilder.setCancelable(false);
        dialogBuilder.setPositiveButton("ADD", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String comment = editText.getText().toString();
                updateImage(comment, true);
            }
        });
        dialogBuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //cancel
                onClickEventListener.onTimerResume();
            }
        });
        dialogBuilder.setNeutralButton("REMOVE COMMENT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                updateImage("", false);
            }
        });
        final AlertDialog dialog = dialogBuilder.create();

        //make positive dialog's button disabled until some text entered
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (s.length() < 1) {
                    positiveButton.setEnabled(false);
                } else {
                    positiveButton.setEnabled(true);
                }
            }
        });
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
    }

    private void updateImage(String comment, boolean isFavorite) {
        image.setComment(comment);
        image.setFavorite(isFavorite);
        Image imageToSave = Image.findById(Image.class, image.getId());
        imageToSave.setComment(comment);
        imageToSave.setFavorite(isFavorite);
        imageToSave.save();
        textView.setText(comment);
        if (isFavorite) {
            addToFavoriteButton.setImageResource(R.mipmap.ic_favorite_fill);
        } else {
            addToFavoriteButton.setImageResource(R.mipmap.ic_favorite_empty);
        }
        onClickEventListener.onTimerResume();
    }

    //interface for activity and  fragment interaction
    public interface OnImageClickEventListener {
        //stops timer fot autoplay in activity if exist
        public void onTimerPause();
        //resume timer for autoplay in activity
        public void onTimerResume();

        public void showActionbBar();
    }
}
