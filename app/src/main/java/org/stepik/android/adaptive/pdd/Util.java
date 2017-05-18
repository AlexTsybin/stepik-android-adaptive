package org.stepik.android.adaptive.pdd;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;
import com.caverock.androidsvg.SVG;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.vk.sdk.VKSdk;

import org.stepik.android.adaptive.pdd.api.API;
import org.stepik.android.adaptive.pdd.data.AnalyticMgr;
import org.stepik.android.adaptive.pdd.data.SharedPreferenceMgr;
import org.stepik.android.adaptive.pdd.ui.activity.StudyActivity;
import org.stepik.android.adaptive.pdd.ui.activity.IntroActivity;
import org.stepik.android.adaptive.pdd.ui.fragment.FragmentMgr;
import org.stepik.android.adaptive.pdd.util.svg.SvgDecoder;
import org.stepik.android.adaptive.pdd.util.svg.SvgDrawableTranscoder;
import org.stepik.android.adaptive.pdd.util.svg.SvgSoftwareLayerSetter;

import java.io.InputStream;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class Util {
    public static void initMgr(final Context context) {
        Config.init(context);
        API.init();
        FragmentMgr.init();
        SharedPreferenceMgr.init(context);
        VKSdk.initialize(context);
        AnalyticMgr.init(context);
    }



    private final static String SVG_EXTENSION = ".svg";


    public static String prepareHTML(final String html) {
        return "<html>" +
                "<head>" +
                "<link rel=\"stylesheet\" type=\"text/css\" href=\"quiz-card.css\" />" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />" +
                "</head>" +
                "<body>" +
                "<div class=\"main\">" +
                html +
                "</div></body></html>";
    }

    public static void hideSoftKeyboard(final Activity a) {
        final View view = a.getCurrentFocus();
        if (view != null) {
            Log.d("hideSoftKeyboard", "hideSoftKeyboard");
            final InputMethodManager mgr = (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);
            mgr.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    public static void loadImageFromNetworkAsync(final String path, final ImageView view, final int placeholder) {
        final Context context = view.getContext();
        if (path.endsWith(SVG_EXTENSION)) {
            GenericRequestBuilder<Uri, InputStream, SVG, PictureDrawable> requestBuilder = Glide
                    .with(context.getApplicationContext())
                    .using(Glide.buildStreamModelLoader(Uri.class, context), InputStream.class)
                    .from(Uri.class)
                    .as(SVG.class)
                    .transcode(new SvgDrawableTranscoder(), PictureDrawable.class)
                    .sourceEncoder(new StreamEncoder())
                    .cacheDecoder(new FileToStreamDecoder<>(new SvgDecoder()))
                    .decoder(new SvgDecoder())
//                    .placeholder(placeholder)
                    .listener(new SvgSoftwareLayerSetter());

            Uri uri = Uri.parse(path);
            requestBuilder
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .load(uri)
                    .into(view);
        } else {
            Glide
                    .with(context.getApplicationContext())
                    .load(path)
                    .asBitmap()
//                    .placeholder(placeholder)
                    .into(view);
        }
    }

    public static void startStudy(final Activity activity) {
        Observable.fromCallable(SharedPreferenceMgr.getInstance()::isNotFirstTime)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((notFirstTime) -> {
                    final Intent intent = new Intent(activity, notFirstTime ? StudyActivity.class : IntroActivity.class);
                    activity.startActivity(intent);
                    activity.finish();
                });
    }

    public static boolean checkPlayServices(final Context context) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        return resultCode == ConnectionResult.SUCCESS;
    }
}