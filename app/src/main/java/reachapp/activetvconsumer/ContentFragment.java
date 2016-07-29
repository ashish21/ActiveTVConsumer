package reachapp.activetvconsumer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import org.jsoup.Connection;
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
        final ContentFragment fragment = new ContentFragment();
        final Bundle args = new Bundle();
        args.putString("type", type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final Bundle args = getArguments();
        if (args == null)
            return null;
        final String type = args.getString("type");
        mListener.setTitle(type);

        final View rootView = inflater.inflate(R.layout.fragment_content, container, false);
        final Activity activity = getActivity();
        final MixpanelAPI mixpanel = MixpanelAPI.getInstance(activity, "944ba55b0438792632412369f541b1b3");

        final RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        final List<Video> list = new ArrayList<>();
        final ContentAdapter contentAdapter = new ContentAdapter(list, mixpanel);
        recyclerView.setAdapter(contentAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));

        final SlideInUpAnimator animator = new SlideInUpAnimator(new DecelerateInterpolator());
        final int dur = 150;
        animator.setAddDuration(dur);
        animator.setRemoveDuration(dur);
        animator.setMoveDuration(dur);
        animator.setChangeDuration(dur);
        recyclerView.setItemAnimator(animator);

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
            final Video video = list.get(position);
            holder.fileName.setText(video.getFileName());
            Glide
                .with(holder.itemView.getContext())
                .load(video.getThumbURL())
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

            final Uri myUri = Uri.parse(file);
            final Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
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
        void setTitle(String title);
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
                final String basePath = "http://192.168.43.1:1993";
                final Document doc = Jsoup.connect(basePath + "/" + strings[0]).get();
                final Elements filesElements = doc.getElementsByClass("files").select("a");
                final List<Video> list = new ArrayList<>();
                Video video;
                Element fileElement;

                final Connection.Response thumbResponse = Jsoup.connect(basePath + "/" + strings[0] + "/.thumbnails")
                        .ignoreHttpErrors(true).execute();
                final int thumbStatus = thumbResponse.statusCode();
                if (thumbStatus == 200) {
                    final Document thumbDoc = thumbResponse.parse();
                    final Elements thumbFilesElements = thumbDoc.getElementsByClass("files").select("a");
                    Element thumbFileElement;
                    for (int i = 0; i<filesElements.size(); i++) {
                        fileElement = filesElements.get(i);
                        thumbFileElement = thumbFilesElements.get(i);
                        video = new Video(fileElement.select("span").html(), basePath +
                                fileElement.attr("href"), basePath + thumbFileElement.attr("href"));
                        list.add(video);
                    }
                }
                else {
                    for (int i = 0; i<filesElements.size(); i++) {
                        fileElement = filesElements.get(i);
                        video = new Video(fileElement.select("span").html(), basePath +
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
