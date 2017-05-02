package org.stepik.droid.adaptive.pdd.ui.adapter;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.webkit.WebSettings;

import org.stepik.droid.adaptive.pdd.data.model.Attempt;
import org.stepik.droid.adaptive.pdd.data.model.Submission;
import org.stepik.droid.adaptive.pdd.databinding.FragmentRecommendationsBinding;
import org.stepik.droid.adaptive.pdd.ui.DefaultWebViewClient;
import org.stepik.droid.adaptive.pdd.ui.helper.AnimationHelper;
import org.stepik.droid.adaptive.pdd.ui.helper.LayoutHelper;
import org.stepik.droid.adaptive.pdd.ui.listener.OnCardSwipeListener;
import org.stepik.droid.adaptive.pdd.ui.view.QuizCardView;

public final class QuizCardAdapter {
    private static final long ANIMATION_DURATION = 450;
    private static final long ANIMATION_DURATION_FAST = 300;

    private static final int ANSWER_ANIMATION_START = -160;
    private static final int ANSWER_ANIMATION_END = -32;
    private static final int SOLVE_BUTTON_OFFSET = 24;

    private ViewPropertyAnimator animatorHard;
    private ViewPropertyAnimator animatorEasy;

    private ValueAnimator answerAnimator, answerAnimatorReverse;

    private final String TAG = "QuizCardAdapter";

    private FragmentRecommendationsBinding binding;

    private final OnCardSwipeListener listener;
    private final AttemptAnswersAdapter attemptAnswersAdapter;
    private final Context context;

    private final int screenHeight;

    public enum State {
        PENDING_FOR_NEXT_RECOMMENDATION,
        RECOMMENDATION_LOADED,
        PENDING_FOR_ANSWERS,
        ANSWERS_LOADED,
        PENDING_FOR_SUBMISSION,
        SUBMISSION_CORRECT,
        SUBMISSION_WRONG
    }

    private State state = State.PENDING_FOR_NEXT_RECOMMENDATION;


    
    public QuizCardAdapter(final Context context, final OnCardSwipeListener listener) {
        this.context = context;
        this.listener = listener;
        this.attemptAnswersAdapter = new AttemptAnswersAdapter();

        this.screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    public void bind(final FragmentRecommendationsBinding binding) {
        this.binding = binding;

        final WebSettings settings = binding.fragmentRecommendationsQuestion.getSettings();
        settings.setAllowContentAccess(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);


        binding.fragmentRecommendationsQuestion.setWebViewClient(new DefaultWebViewClient(null, (v, u) ->
                setUIState(State.RECOMMENDATION_LOADED)));
        binding.fragmentRecommendationsQuestion.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        binding.fragmentRecommendationsContainer.setQuizCardFlingListener(new QuizCardView.QuizCardFlingListener() {
            @Override
            public void onFlingDown() {
                if (binding.fragmentRecommendationsSolve.getVisibility() == View.VISIBLE) {
                    binding.fragmentRecommendationsSolve.callOnClick();
                }
            }

            @Override
            public void onScroll(final float scrollProgress) {
                if (Math.abs(scrollProgress) > 0.5) {
                    if (scrollProgress < 0) {
                        if (animatorEasy == null) {
                            animatorEasy = AnimationHelper
                                    .createReactionAppearAnimation(binding.fragmentRecommendationsEasyReaction);
                        }
                    } else {
                        if (animatorHard == null) {
                            animatorHard = AnimationHelper
                                    .createReactionAppearAnimation(binding.fragmentRecommendationsHardReaction);
                        }
                    }
                } else {
                    hideReactionAnimation(0);
                }
            }

            @Override
            public void onSwiped() {
                setUIState(State.PENDING_FOR_NEXT_RECOMMENDATION);
            }

            @Override
            public void onSwipeLeft() {
                animatorEasy = AnimationHelper.createReactionAppearAnimation(binding.fragmentRecommendationsEasyReaction)
                        .withEndAction(() -> hideReactionAnimation(ANIMATION_DURATION * 2));
                listener.onCardSwipe(OnCardSwipeListener.SWIPE_DIRECTION.LEFT);
            }

            @Override
            public void onSwipeRight() {
                animatorHard = AnimationHelper.createReactionAppearAnimation(binding.fragmentRecommendationsHardReaction)
                        .withEndAction(() -> hideReactionAnimation(ANIMATION_DURATION * 2));
                listener.onCardSwipe(OnCardSwipeListener.SWIPE_DIRECTION.RIGHT);
            }

            @Override
            public void onSwipeDown() {
                AnimationHelper.createReactionAppearAnimation(binding.fragmentRecommendationsCorrectReaction)
                        .withEndAction(() -> hideReactionAnimation(ANIMATION_DURATION * 2));
            }
        });

        binding.fragmentRecommendationsCard.bringToFront();
        binding.fragmentRecommendationsSolve.bringToFront();

        final ValueAnimator.AnimatorUpdateListener answerAnimatorUpdateListener =
                AnimationHelper.createLayoutMarginAnimation(binding.fragmentRecommendationsAnswersContainer);

        answerAnimator = ValueAnimator.ofInt(
                LayoutHelper.pxFromDp(context, ANSWER_ANIMATION_START),
                LayoutHelper.pxFromDp(context, ANSWER_ANIMATION_END)
        );
        answerAnimator.setDuration(ANIMATION_DURATION_FAST);
        answerAnimator.addUpdateListener(answerAnimatorUpdateListener);

        answerAnimatorReverse = ValueAnimator.ofInt();
        answerAnimatorReverse.setDuration(ANIMATION_DURATION_FAST);
        answerAnimatorReverse.addUpdateListener((anm) ->
                binding.fragmentRecommendationsAnswersContainer.setAlpha(1f - (float) anm.getCurrentPlayTime() / anm.getDuration()));
        answerAnimatorReverse.addUpdateListener(answerAnimatorUpdateListener);


        attemptAnswersAdapter.setSubmitButton(binding.fragmentRecommendationsSubmit);
        binding.fragmentRecommendationsAnswers.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext()));
        binding.fragmentRecommendationsAnswers.setAdapter(attemptAnswersAdapter);

        setUIState(state);
    }

    public void unbind() {
        binding = null;
    }



    public void setAttempt(final Attempt attempt) {
        attemptAnswersAdapter.setAttempt(attempt);
        setUIState(State.ANSWERS_LOADED);
    }

    public Submission getSubmission() { return attemptAnswersAdapter.getSubmission(); }

    public void setSubmission(final Submission submission) {
        switch (submission.getStatus()) {
            case CORRECT:
                setUIState(State.SUBMISSION_CORRECT);
            break;
            case WRONG:
                setUIState(State.SUBMISSION_WRONG);
            break;
            default:
                Log.e(TAG, "Wrong submission state: " + submission.getStatus());
        }
    }

    private void hideReactionAnimation(final long delay) {
        if (animatorEasy != null) {
            AnimationHelper.createReactionDisappearAnimation(binding.fragmentRecommendationsEasyReaction)
                    .setStartDelay(delay);
            animatorEasy = null;
        }

        if (animatorHard != null) {
            AnimationHelper.createReactionDisappearAnimation(binding.fragmentRecommendationsHardReaction)
                    .setStartDelay(delay);
            animatorHard = null;
        }

        AnimationHelper.createReactionDisappearAnimation(binding.fragmentRecommendationsCorrectReaction)
                .setStartDelay(delay);
    }


    private void pendingForNextRecommendation() {
        binding.fragmentRecommendationsContainer.setTranslationX(0);
        binding.fragmentRecommendationsContainer.setTranslationY(-screenHeight);
        binding.fragmentRecommendationsProgressBar.setVisibility(View.VISIBLE);

        binding.fragmentRecommendationsAnswersContainer.setVisibility(View.GONE);
        binding.fragmentRecommendationsSolve.setVisibility(View.VISIBLE);
    }

    private void recommendationLoaded() {
        binding.fragmentRecommendationsProgressBar.setVisibility(View.GONE);

        final ObjectAnimator animator =
                ObjectAnimator.ofFloat(binding.fragmentRecommendationsContainer, "translationY", 0);
        animator.setInterpolator(AnimationHelper.OvershootInterpolator2F);
        animator.setDuration(ANIMATION_DURATION);
        animator.addUpdateListener((anm) ->
            LayoutHelper.wrapWebView(binding.fragmentRecommendationsCard,
                    binding.fragmentRecommendationsQuestion, SOLVE_BUTTON_OFFSET));
        animator.start();
    }

    private void pendingForAnswers() {
        binding.fragmentRecommendationsSolve.setVisibility(View.GONE);
        binding.fragmentRecommendationsSubmit.setVisibility(View.VISIBLE);

        LayoutHelper.wrapWebView(binding.fragmentRecommendationsCard,
                binding.fragmentRecommendationsQuestion, 0);

        binding.fragmentRecommendationsSubmit.setAlpha(1);
        binding.fragmentRecommendationsSubmit.setEnabled(false);

        binding.fragmentRecommendationsAnswersProgress.setVisibility(View.VISIBLE);
        binding.fragmentRecommendationsAnswers.setVisibility(View.GONE);

        binding.fragmentRecommendationsAnswersContainer.setVisibility(View.VISIBLE);
        binding.fragmentRecommendationsAnswersContainer.setAlpha(1);
        answerAnimator.start();
    }

    private void answersLoaded() {
        binding.fragmentRecommendationsAnswersProgress.setVisibility(View.GONE);
        binding.fragmentRecommendationsAnswers.setVisibility(View.VISIBLE);
    }

    private void pendingForSubmissions() {
        binding.fragmentRecommendationsSubmit.setEnabled(false);
        binding.fragmentRecommendationsAnswersProgress.setVisibility(View.VISIBLE);
        binding.fragmentRecommendationsAnswers.setVisibility(View.GONE);
    }

    private void submissionCorrect() {
        answerAnimatorReverse.setIntValues(
                LayoutHelper.pxFromDp(context, ANSWER_ANIMATION_END),
                -binding.fragmentRecommendationsAnswersContainer.getHeight());
        answerAnimatorReverse.start();

        binding.fragmentRecommendationsSubmit.animate()
                .alpha(0)
                .setDuration(ANIMATION_DURATION_FAST)
                .withEndAction(binding.fragmentRecommendationsContainer::swipeDown);
    }

    private void submissionWrong() {
        AnimationHelper.playWiggleAnimation(binding.fragmentRecommendationsContainer);

        binding.fragmentRecommendationsAnswersProgress.setVisibility(View.GONE);
        binding.fragmentRecommendationsAnswers.setVisibility(View.VISIBLE);

    }

    public void setUIState(final State state) {
        Log.d(TAG, "Switch state: " + state);
        if (binding == null) return;
        switch (state) {
            case PENDING_FOR_NEXT_RECOMMENDATION:
                pendingForNextRecommendation();
            break;
            case RECOMMENDATION_LOADED:
                recommendationLoaded();
            break;
            case PENDING_FOR_ANSWERS:
                pendingForAnswers();
            break;
            case ANSWERS_LOADED:
                answersLoaded();
            break;
            case PENDING_FOR_SUBMISSION:
                pendingForSubmissions();
            break;
            case SUBMISSION_CORRECT:
                submissionCorrect();
            break;
            case SUBMISSION_WRONG:
                submissionWrong();
            break;
        }
        this.state = state;
    }
}
