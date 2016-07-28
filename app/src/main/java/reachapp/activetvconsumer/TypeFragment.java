package reachapp.activetvconsumer;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;


public class TypeFragment extends Fragment {

    private OnTypeFragmentInteractionListener mListener;

    public TypeFragment() {}

    static TypeFragment newInstance() {
        return new TypeFragment();
    }

//    private final List<String> list = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_type, container, false);
        final AppCompatActivity activity = (AppCompatActivity) getActivity();
        final ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(getResources().getString(R.string.app_name));
        final RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        final List<String> list = new ArrayList<>();
        final ContentAdapter contentAdapter = new ContentAdapter(list, mListener);
        recyclerView.setAdapter(contentAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(activity, 2));
        SlideInUpAnimator animator = new SlideInUpAnimator(new DecelerateInterpolator());
        animator.setAddDuration(150);
        animator.setRemoveDuration(150);
        animator.setMoveDuration(150);
        animator.setChangeDuration(150);
        recyclerView.setItemAnimator(animator);
        new GetTypes(contentAdapter, list).execute();

        return rootView;
    }

    private static class ContentAdapter extends RecyclerView.Adapter<ContentViewHolder>
            implements OnContentClickListener {

        private final List<String> list;
        private final OnTypeFragmentInteractionListener mListener;

        private ContentAdapter(List<String> list, OnTypeFragmentInteractionListener mListener) {
            super();
            this.list = list;
            this.mListener = mListener;
        }

        @Override
        public ContentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ContentViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_type, parent, false), this);
        }

        @Override
        public void onBindViewHolder(ContentViewHolder holder, int position) {
            final String type = list.get(position);
            holder.fileName.setText(type);
            switch (type) {
                case "Apps":
                    Glide
                        .with(holder.cardBG.getContext())
                        .load(R.drawable.apps_bg)
                        .centerCrop()
                        .placeholder(R.drawable.example_type_bg)
                        .crossFade()
                        .into(holder.cardBG);
                    break;
                case "Movies":
                    Glide
                        .with(holder.cardBG.getContext())
                        .load(R.drawable.movies_bg)
                        .centerCrop()
                        .placeholder(R.drawable.example_type_bg)
                        .crossFade()
                        .into(holder.cardBG);
                    break;
                case "Videos":
                    Glide
                        .with(holder.cardBG.getContext())
                        .load(R.drawable.videos_bg)
                        .centerCrop()
                        .placeholder(R.drawable.example_type_bg)
                        .crossFade()
                        .into(holder.cardBG);
                    break;
                case "Music":
                    Glide
                        .with(holder.cardBG.getContext())
                        .load(R.drawable.music_bg)
                        .centerCrop()
                        .placeholder(R.drawable.example_type_bg)
                        .crossFade()
                        .into(holder.cardBG);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Override
        public void onContentClick(Context context, int position) {
            final String type = list.get(position);
            mListener.onOpenContent(type);
        }
    }

    private static class ContentViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{

        private final OnContentClickListener onContentClickListener;
        private final TextView fileName;
        private final ImageView cardBG;

        private ContentViewHolder(View itemView, OnContentClickListener onContentClickListener) {
            super(itemView);
            this.itemView.setOnClickListener(this);
            this.onContentClickListener = onContentClickListener;
            this.fileName = (TextView) this.itemView.findViewById(R.id.fileName);
            this.cardBG = (ImageView) this.itemView.findViewById(R.id.card_bg);
        }

        @Override
        public void onClick(View view) {
            onContentClickListener.onContentClick(view.getContext(), getAdapterPosition());
        }
    }

    interface OnContentClickListener {
        void onContentClick(Context context, int position);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnTypeFragmentInteractionListener) {
            mListener = (OnTypeFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnTypeFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    interface OnTypeFragmentInteractionListener {
        void onOpenContent(String type);
    }

    private static class GetTypes extends AsyncTask<Void, Void, List<String>> {

        private ContentAdapter contentAdapter;
        private List<String> list;

        private GetTypes(ContentAdapter contentAdapter, List<String> list) {
            super();
            this.contentAdapter = contentAdapter;
            this.list = list;
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            try {
                final Document doc = Jsoup.connect("http://192.168.43.1:1993/").get();
                final Elements filesElements = doc.getElementsByClass("directories").select("a");
                final List<String> list = new ArrayList<>();
                Element fileElement;
                for (int i = 0; i<filesElements.size(); i++) {
                    fileElement = filesElements.get(i);
                    list.add(fileElement.select("span").html().replace("/",""));
                }
                return list;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<String> newList) {
            super.onPostExecute(newList);
            if (list.equals(newList))
                return;
            list.clear();
            list.addAll(newList);
            contentAdapter.notifyItemRangeChanged(0, list.size());
        }
    }
}
