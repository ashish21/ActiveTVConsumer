package reachapp.activetvconsumer;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_type, container, false);

        mListener.setTitle(getResources().getString(R.string.app_name));
        mListener.showBackBtn(false);
        final RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        final LinearLayout searchBox = (LinearLayout) rootView.findViewById(R.id.searchBox);
        final List<Type> list = new ArrayList<>();
        final ContentAdapter contentAdapter = new ContentAdapter(list, searchBox, mListener);
        recyclerView.setAdapter(contentAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        final SlideInUpAnimator animator = new SlideInUpAnimator(new DecelerateInterpolator());
        final int dur = 150;
        animator.setAddDuration(dur);
        animator.setRemoveDuration(dur);
        animator.setMoveDuration(dur);
        animator.setChangeDuration(dur);
        recyclerView.setItemAnimator(animator);

        new GetTypes(contentAdapter, searchBox, list).execute();

        return rootView;
    }

    private static class ContentAdapter extends RecyclerView.Adapter<ContentViewHolder>
            implements OnContentClickListener {

        private final List<Type> list;
        private final LinearLayout searchBox;
        private final OnTypeFragmentInteractionListener mListener;

        private ContentAdapter(List<Type> list, LinearLayout searchBox, OnTypeFragmentInteractionListener mListener) {
            super();
            this.list = list;
            this.searchBox = searchBox;
            this.mListener = mListener;
        }

        @Override
        public ContentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ContentViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_type, parent, false), this);
        }

        @Override
        public void onBindViewHolder(ContentViewHolder holder, int position) {
            final Type type = list.get(position);
            holder.typeText.setText(type.getTypeName());
            Glide
                .with(holder.itemView.getContext())
                .load(type.getThumbURL())
                .centerCrop()
                .placeholder(R.drawable.example_type_bg)
                .into(holder.cardBG);
        }

        @Override
        public int getItemCount() {
            final int count = list.size();
            searchBox.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            return count;
        }

        @Override
        public void onContentClick(int position) {
            mListener.onOpenContent(list.get(position).getTypeName());
        }
    }

    private static class ContentViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{

        private final OnContentClickListener onContentClickListener;
        private final TextView typeText;
        private final ImageView cardBG;

        private ContentViewHolder(View itemView, OnContentClickListener onContentClickListener) {
            super(itemView);
            this.itemView.setOnClickListener(this);
            this.onContentClickListener = onContentClickListener;
            this.typeText = (TextView) this.itemView.findViewById(R.id.typeText);
            this.cardBG = (ImageView) this.itemView.findViewById(R.id.card_bg);
        }

        @Override
        public void onClick(View view) {
            onContentClickListener.onContentClick(getAdapterPosition());
        }
    }

    interface OnContentClickListener {
        void onContentClick(int position);
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
        void setTitle(String title);
        void showBackBtn(boolean show);
    }

    private static class GetTypes extends AsyncTask<Void, Void, List<Type>> {

        private final ContentAdapter contentAdapter;
        private final LinearLayout searchBox;
        private final List<Type> list;

        private GetTypes(ContentAdapter contentAdapter, LinearLayout searchBox, List<Type> list) {
            super();
            this.contentAdapter = contentAdapter;
            this.searchBox = searchBox;
            this.list = list;
        }

        @Override
        protected List<Type> doInBackground(Void... voids) {
            final String path = "http://192.168.43.1:1993";
            int retry = 0;
            do {
                retry++;
                Log.d("Ashish", "Try count " + retry);
                try {
                    final Document doc = Jsoup.connect(path + "/").get();
                    final Elements filesElements = doc.getElementsByClass("directories")
                            .select("a");
                    if (filesElements.size() == 0)
                        return null;
                    filesElements.remove(0);
                    final List<Type> list = new ArrayList<>();
                    Element fileElement;
                    Type type;
                    final Document thumbDoc = Jsoup.connect(path + "/.thumbnails").get();
                    final Elements thumbFilesElements = thumbDoc.getElementsByClass("files").select("a");
                    Element thumbFileElement;

                    for (int i = 0; i < filesElements.size(); i++) {
                        fileElement = filesElements.get(i);
                        if (thumbFilesElements.size() > i) {
                            thumbFileElement = thumbFilesElements.get(i);
                            type = new Type(fileElement.select("span").html().replace("/", ""), path
                                    + thumbFileElement.attr("href"));
                        } else
                            type = new Type(fileElement.select("span").html().replace("/", ""), null);
                        list.add(type);
                    }
                    return list;
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            } while (retry < 10);
            return null;
        }

        @Override
        protected void onPostExecute(List<Type> newList) {
            super.onPostExecute(newList);
            list.clear();
            if (newList != null)
                list.addAll(newList);
            else {
                final ProgressBar searchBar = (ProgressBar) searchBox.findViewById(R.id.searchBar);
                searchBar.setVisibility(View.GONE);
                final TextView searchText = (TextView) searchBox.findViewById(R.id.searchText);
                searchText.setText("Could not connect to any seeder.\n\nCLICK TO RETRY");
                searchBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        searchBar.setVisibility(View.VISIBLE);
                        searchText.setText("Searching for free videos, movies, music and more...");
                        new GetTypes(contentAdapter, searchBox, list).execute();
                    }
                });
            }
            contentAdapter.notifyItemRangeChanged(0, list.size());
        }
    }
}
