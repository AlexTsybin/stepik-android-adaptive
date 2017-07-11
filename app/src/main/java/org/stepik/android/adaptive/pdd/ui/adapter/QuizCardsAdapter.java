package org.stepik.android.adaptive.pdd.ui.adapter;

import android.databinding.DataBindingUtil;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.stepik.android.adaptive.pdd.R;
import org.stepik.android.adaptive.pdd.core.presenter.CardPresenter;
import org.stepik.android.adaptive.pdd.data.model.Card;
import org.stepik.android.adaptive.pdd.ui.fragment.CardsFragment;
import org.stepik.android.adaptive.pdd.ui.listener.AdaptiveReactionListener;
import org.stepik.android.adaptive.pdd.ui.listener.ExperienceListener;
import org.stepik.android.adaptive.pdd.ui.view.QuizCardsContainer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class QuizCardsAdapter extends QuizCardsContainer.CardsAdapter<QuizCardViewHolder> {
    private List<CardPresenter> presenters = new ArrayList<>();
    private final AdaptiveReactionListener listener;
    private final ExperienceListener experienceListener;

    public QuizCardsAdapter(AdaptiveReactionListener listener, ExperienceListener experienceListener) {
        this.listener = listener;
        this.experienceListener = experienceListener;
    }

    private WeakReference<CardsFragment> fragmentWeakReference = new WeakReference<>(null);

    public void attachFragment(CardsFragment fragment) {
        fragmentWeakReference = new WeakReference<>(fragment);
    }

    public void recycle() {
        fragmentWeakReference.clear();
        for (final CardPresenter presenter : presenters) {
            presenter.destroy();
        }
    }

    /**
     * Method that being called onDestroyView to properly detach view from presenters
     */
    public void detach() {
        for (final CardPresenter presenter : presenters) {
            presenter.detachView();
        }
    }

    public boolean isCardExists(long lessonId) {
        for (CardPresenter presenter : presenters) {
            if (presenter.getCard().getLessonId() == lessonId) return true;
        }
        return false;
    }

    @Override
    protected QuizCardViewHolder onCreateViewHolder(ViewGroup parent) {
        return new QuizCardViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.quiz_card_view, parent, false));
    }

    @Override
    public int getItemCount() {
        return presenters.size();
    }

    @Override
    protected void onBindViewHolder(QuizCardViewHolder holder, int pos) {
        holder.bind(presenters.get(pos));
    }

    @Override
    protected void onBindTopCard(QuizCardViewHolder holder, int pos) {
        holder.onTopCard();
    }

    public void add(Card card) {
        presenters.add(new CardPresenter(card, listener, experienceListener));
        notifyDataAdded();
    }

    @Override
    protected void poll() {
        presenters.remove(0).destroy();
    }
}
