package org.stepik.android.adaptive.ui.view.container;


import android.view.View;

public interface ContainerView {
    void setAdapter(ContainerAdapter adapter);

    void onDataSetChanged();
    void onDataAdded();
    void onRebind();
    void onRebind(int pos);

    class ViewHolder {
        private final View view;
        private boolean attached = false;

        public ViewHolder(View view) {
            this.view = view;
        }

        public void setAttached(boolean attached) {
            this.attached = attached;
        }

        public boolean isAttached() {
            return attached;
        }

        public View getView() {
            return view;
        }
    }
}
