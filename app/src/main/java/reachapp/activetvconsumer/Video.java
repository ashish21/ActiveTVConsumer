package reachapp.activetvconsumer;

/**
 * Created by ashish on 23/07/16.
 */

class Video {

    private String fileName;
    private String fileURL;
    private String thumbURL;

    @Override
    public String toString() {
        return "Video{" +
                "fileName='" + fileName + '\'' +
                ", fileURL='" + fileURL + '\'' +
                ", thumbURL='" + thumbURL + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Video video = (Video) o;

        if (fileName != null ? !fileName.equals(video.fileName) : video.fileName != null)
            return false;
        if (fileURL != null ? !fileURL.equals(video.fileURL) : video.fileURL != null) return false;
        return thumbURL != null ? thumbURL.equals(video.thumbURL) : video.thumbURL == null;

    }

    @Override
    public int hashCode() {
        int result = fileName != null ? fileName.hashCode() : 0;
        result = 31 * result + (fileURL != null ? fileURL.hashCode() : 0);
        result = 31 * result + (thumbURL != null ? thumbURL.hashCode() : 0);
        return result;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileURL(String fileURL) {
        this.fileURL = fileURL;
    }

    public void setThumbURL(String thumbURL) {
        this.thumbURL = thumbURL;
    }

    String getFileName() {

        return fileName;
    }

    String getFileURL() {
        return fileURL;
    }

    String getThumbURL() {
        return thumbURL;
    }

    Video(String fileName, String fileURL, String thumbURL) {

        this.fileName = fileName;
        this.fileURL = fileURL;
        this.thumbURL = thumbURL;
    }
}
