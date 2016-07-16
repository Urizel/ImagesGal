package com.spb.kbv.imagesgal;

import android.os.Parcel;
import android.os.Parcelable;

import com.orm.SugarRecord;

public class Image extends SugarRecord implements Parcelable{
    private int image_id;
    private int number;
    private String url;
    private boolean favorite;
    private String comment;

    public Image(){}

    public Image(int image_id, int number, String url) {
        this.image_id = image_id;
        this.number = number;
        this.url = url;
    }

    public int getImageId() {
        return image_id;
    }

    public void setImageId(int image_id) {
        this.image_id = image_id;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(image_id);
        dest.writeInt(number);
        dest.writeString(url);
        dest.writeByte((byte) (favorite ? 1 : 0));
        dest.writeString(comment);
    }

    private Image(Parcel parcel) {
        image_id = parcel.readInt();
        number = parcel.readInt();
        url = parcel.readString();
        favorite = parcel.readByte() == 1;
        comment = parcel.readString();
    }

    public static final Creator<Image> CREATOR = new Creator<Image>() {
        @Override
        public Image createFromParcel(Parcel source) {
            return new Image(source);
        }

        @Override
        public Image[] newArray(int size) {
            return new Image[size];
        }
    };

}
