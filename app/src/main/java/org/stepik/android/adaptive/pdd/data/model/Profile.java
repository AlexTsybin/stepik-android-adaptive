package org.stepik.android.adaptive.pdd.data.model;

public final class Profile {
    private long id;
    private long bit_field;
    private long level;
    private String first_name;
    private String last_name;
    private boolean is_private;
    private String avatar;
    private String language;
    private String short_bio;
    private String details;
    private String notification_email_delay;
    private String level_title;
    private boolean subscribed_for_mail;
    private boolean is_staff;
    private boolean is_guest;
    private boolean can_add_lesson;
    private boolean can_add_course;
    private boolean can_add_group;
    private boolean subscribed_for_news_en;
    private boolean subscribed_for_news_ru;
    private long[] emailAddresses;

    public long getId() {
        return id;
    }

    public String getFirstName() {
        return first_name;
    }

    public String getLastName() {
        return last_name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setSubscribed_for_mail(boolean subscribed_for_mail) {
        this.subscribed_for_mail = subscribed_for_mail;
    }
}
