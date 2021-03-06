package com.github.ayltai.newspaper.model;

import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

@SuppressWarnings("checkstyle:methodcount")
public class Item extends RealmObject implements Cloneable, Comparable<Item>, Parcelable {
    //region Constants

    public static final String FIELD_SOURCE       = "source";
    public static final String FIELD_CATEGORY     = "category";
    public static final String FIELD_LINK         = "link";
    public static final String FIELD_PUBLISH_DATE = "publishDate";
    public static final String FIELD_VIDEO        = "video";
    public static final String FIELD_BOOKMARKED   = "bookmarked";

    //endregion

    //region Fields

    @PrimaryKey
    private String  link;
    private String  title;
    private String  description;
    private boolean isFullDescription;
    private long    publishDate;
    private String  source;
    private String  category;
    private Video   video;
    private boolean bookmarked;

    private RealmList<Image> images = new RealmList<>();

    //endregion

    public Item() {
    }

    //region Properties

    @NonNull
    public String getTitle() {
        return this.title;
    }

    public void setTitle(@Nullable final String title) {
        this.title = title == null ? null : title.trim();
    }

    @Nullable
    public String getDescription() {
        return this.description;
    }

    public void setDescription(@Nullable final String description) {
        this.description = description == null ? null : description.trim();
    }

    public boolean isFullDescription() {
        return this.isFullDescription;
    }

    public void setIsFullDescription(final boolean isFullDescription) {
        this.isFullDescription = isFullDescription;
    }

    @NonNull
    public String getLink() {
        return this.link;
    }

    public void setLink(@Nullable final String link) {
        this.link = link;
    }

    @Nullable
    public Date getPublishDate() {
        if (this.publishDate == 0) return null;

        return new Date(this.publishDate);
    }

    public void setPublishDate(@Nullable final Date publishDate) {
        this.publishDate = publishDate == null ? 0 : publishDate.getTime();
    }

    @NonNull
    public String getSource() {
        return this.source;
    }

    public void setSource(@NonNull final String source) {
        this.source = source.trim();
    }

    @NonNull
    public String getCategory() {
        return this.category;
    }

    public void setCategory(@NonNull final String category) {
        this.category = category;
    }

    @NonNull
    public RealmList<Image> getImages() {
        return this.images;
    }

    @Nullable
    public Video getVideo() {
        return this.video;
    }

    public void setVideo(@Nullable final Video video) {
        this.video = video;
    }

    public boolean isBookmarked() {
        return this.bookmarked;
    }

    public void setBookmarked(final boolean bookmarked) {
        this.bookmarked = bookmarked;
    }

    //endregion

    @Override
    public final int compareTo(@NonNull final Item item) {
        if (this.link.equals(item.link)) return 0;

        if (this.publishDate != 0 && item.publishDate != 0) return (int)(item.publishDate - this.publishDate);

        return this.title.compareTo(item.title);
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) return true;

        if (obj instanceof Item) {
            final Item item = (Item)obj;

            return this.link.equals(item.link);
        }

        return false;
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public Item clone() {
        final Item item = new Item();

        item.setTitle(this.title);
        item.setDescription(this.description);
        item.setIsFullDescription(this.isFullDescription);
        item.setLink(this.link);
        item.setPublishDate(new Date(this.publishDate));
        item.getImages().addAll(this.images);
        item.setVideo(this.video);
        item.setBookmarked(this.bookmarked);
        item.setSource(this.source);
        item.setCategory(this.category);

        return item;
    }

    @Override
    public final int hashCode() {
        return this.link.hashCode();
    }

    //region Parcelable

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeString(this.title);
        dest.writeString(this.description);
        dest.writeInt(this.isFullDescription ? 1 : 0);
        dest.writeString(this.link);
        dest.writeLong(this.publishDate);
        dest.writeString(this.source);
        dest.writeString(this.category);
        dest.writeTypedList(this.images);
        dest.writeParcelable(this.video, 0);
        dest.writeInt(this.bookmarked ? 1 : 0);
    }

    protected Item(@NonNull final Parcel in) {
        this.title             = in.readString();
        this.description       = in.readString();
        this.isFullDescription = in.readInt() == 1;
        this.link              = in.readString();
        this.publishDate       = in.readLong();
        this.source            = in.readString();
        this.category          = in.readString();

        this.images = new RealmList<>();
        this.images.addAll(in.createTypedArrayList(Image.CREATOR));

        this.video      = in.readParcelable(Video.class.getClassLoader());
        this.bookmarked = in.readInt() == 1;
    }

    public static final Parcelable.Creator<Item> CREATOR = new Parcelable.Creator<Item>() {
        @NonNull
        @Override
        public Item createFromParcel(@NonNull final Parcel source) {
            return new Item(source);
        }

        @NonNull
        @Override
        public Item[] newArray(final int size) {
            return new Item[size];
        }
    };

    //endregion
}
