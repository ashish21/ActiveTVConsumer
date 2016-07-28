package reachapp.activetvconsumer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;


public class ContentFragment extends Fragment {

    private OnContentFragmentInteractionListener mListener;

    public ContentFragment() {}

    static ContentFragment newInstance(String type) {
        ContentFragment fragment = new ContentFragment();
        Bundle args = new Bundle();
        args.putString("type", type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_content, container, false);
        final AppCompatActivity activity = (AppCompatActivity) getActivity();
        final MixpanelAPI mixpanel = MixpanelAPI.getInstance(activity, "fd461effbd0259aac4dee8a62888f311");
        final RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        final List<Video> list = new ArrayList<>();
        final ContentAdapter contentAdapter = new ContentAdapter(list, mixpanel);
        recyclerView.setAdapter(contentAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        SlideInUpAnimator animator = new SlideInUpAnimator(new DecelerateInterpolator());
        animator.setAddDuration(150);
        animator.setRemoveDuration(150);
        animator.setMoveDuration(150);
        animator.setChangeDuration(150);
        recyclerView.setItemAnimator(animator);
        if (getArguments() == null)
            return null;
        final String type = getArguments().getString("type");
        final ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(type);
        new GetList(contentAdapter, list).execute(type);

        return rootView;
    }

    private static class ContentAdapter extends RecyclerView.Adapter<ContentViewHolder>
            implements OnContentClickListener {

        private final List<Video> list;
        private final MixpanelAPI mixpanelAPI;

        private ContentAdapter(List<Video> list, MixpanelAPI mixpanelAPI) {
            super();
            this.list = list;
            this.mixpanelAPI = mixpanelAPI;
        }

        @Override
        public ContentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ContentViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_content, parent, false), this);
        }

        @Override
        public void onBindViewHolder(ContentViewHolder holder, int position) {
            holder.fileName.setText(list.get(position).getFileName());
            Glide
                .with(holder.thumbnail.getContext())
                .load(list.get(position).getThumbURL())
                .centerCrop()
                .placeholder(R.drawable.example_type_bg)
                .crossFade()
                .into(holder.thumbnail);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Override
        public void onContentClick(Context context, int position) {
            final Video video = list.get(position);
            final Map<String, Object> map = new HashMap<>();
            map.put("fileName", video.getFileName());
            mixpanelAPI.trackMap("Transaction", map);
            final String mime, file = video.getFileURL();
            if (file.contains(".mp3"))
                mime = "audio/mp3";
                else if (file.contains(".mp4") || file.contains(".m4v") || file.contains(".mkv") || file.contains(".avi"))
                mime = "video/*";
            else
                mime = "*/*";

            Uri myUri = Uri.parse(file);
            Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(myUri, mime);
            context.startActivity(intent);
        }
    }

    private static class ContentViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{

        private final OnContentClickListener onContentClickListener;
        private final TextView fileName;
        private final ImageView thumbnail;

        private ContentViewHolder(View itemView, OnContentClickListener onContentClickListener) {
            super(itemView);
            this.itemView.setOnClickListener(this);
            this.onContentClickListener = onContentClickListener;
            this.fileName = (TextView) this.itemView.findViewById(R.id.fileName);
            this.thumbnail = (ImageView) this.itemView.findViewById(R.id.thumbnail);
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
        if (context instanceof OnContentFragmentInteractionListener) {
            mListener = (OnContentFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnContentFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    interface OnContentFragmentInteractionListener {
    }

    private static class GetList extends AsyncTask<String, Void, List<Video>> {

        private ContentAdapter contentAdapter;
        private List<Video> list;

        private GetList(ContentAdapter contentAdapter, List<Video> list) {
            super();
            this.contentAdapter = contentAdapter;
            this.list = list;
        }

        @Override
        protected List<Video> doInBackground(String... strings) {
            try {
                final Document doc = Jsoup.connect("http://192.168.43.1:1993/" + strings[0]).get();
                final Elements filesElements = doc.getElementsByClass("files").select("a");
                final List<Video> list = new ArrayList<>();
                Video video;
                Element fileElement;

                if (strings[0].equals("Movies")) {
                    final Document thumbDoc = Jsoup.connect("http://192.168.43.1:1993/Movies/.thumbnails").get();
                    final Elements thumbFilesElements = thumbDoc.getElementsByClass("files").select("a");
                    Element thumbFileElement;

                    for (int i = 0; i<filesElements.size(); i++) {
                        fileElement = filesElements.get(i);
                        thumbFileElement = thumbFilesElements.get(i);
                        video = new Video(fileElement.select("span").html(), "http://192.168.43.1:1993" +
                                fileElement.attr("href"), "http://192.168.43.1:1993" + thumbFileElement.attr("href"));
                        list.add(video);
                    }
                }
                else if (strings[0].equals("Videos")) {
                    final Document thumbDoc = Jsoup.connect("http://192.168.43.1:1993/Videos/.thumbnails").get();
                    final Elements thumbFilesElements = thumbDoc.getElementsByClass("files").select("a");
                    Element thumbFileElement;

                    for (int i = 0; i<filesElements.size(); i++) {
                        fileElement = filesElements.get(i);
                        thumbFileElement = thumbFilesElements.get(i);
                        video = new Video(fileElement.select("span").html(), "http://192.168.43.1:1993" +
                                fileElement.attr("href"), "http://192.168.43.1:1993" + thumbFileElement.attr("href"));
                        list.add(video);
                    }
                }
                else {
                    for (int i = 0; i < filesElements.size(); i++) {
                        fileElement = filesElements.get(i);
                        video = new Video(fileElement.select("span").html(), "http://192.168.43.1:1993" +
                                fileElement.attr("href"), null);
                        list.add(video);
                    }
                }
                return list;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Video> pairList) {
            super.onPostExecute(pairList);
            list.clear();
            list.addAll(pairList);
            contentAdapter.notifyItemRangeChanged(0, list.size());
        }
    }
}
