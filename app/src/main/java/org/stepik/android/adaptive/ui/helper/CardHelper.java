package org.stepik.android.adaptive.ui.helper;

import android.view.View;
import android.widget.ScrollView;

import org.stepik.android.adaptive.api.API;
import org.stepik.android.adaptive.api.RecommendationsResponse;
import org.stepik.android.adaptive.data.SharedPreferenceMgr;
import org.stepik.android.adaptive.data.model.RecommendationReaction;
import org.stepik.android.adaptive.databinding.QuizCardViewBinding;

import io.reactivex.Completable;
import io.reactivex.Observable;

public class CardHelper {

    private final static int CARDS_IN_CACHE = 6;
    private final static int MIN_CARDS_IN_CACHE = 4;

    public static Observable<RecommendationsResponse> createReactionObservable(final long lesson, final RecommendationReaction.Reaction reaction, final int cacheSize) {
        final Observable<RecommendationsResponse> responseObservable = API.getInstance().getNextRecommendations(CARDS_IN_CACHE);

        if (lesson != 0) {
            final Completable reactionCompletable = API.getInstance()
                    .createReaction(new RecommendationReaction(lesson, reaction, SharedPreferenceMgr.getInstance().getProfileId()));
            if (cacheSize <= MIN_CARDS_IN_CACHE) {
                return reactionCompletable.andThen(responseObservable);
            } else {
                return reactionCompletable.toObservable();
            }
        }
        return responseObservable;
    }

    public static void resetSupplementalActions(final QuizCardViewBinding binding) {
        binding.next.setVisibility(View.GONE);
        binding.correct.setVisibility(View.GONE);
        binding.wrong.setVisibility(View.GONE);
        binding.wrongRetry.setVisibility(View.GONE);
        binding.answersProgress.setVisibility(View.GONE);
        binding.hint.setVisibility(View.GONE);
        binding.submit.setVisibility(View.VISIBLE);
    }

    public static void scrollDown(final ScrollView view) {
        view.post(() -> view.fullScroll(View.FOCUS_DOWN));
    }
}