package reachapp.activetvconsumer;

/**
 * Created by ashish on 23/07/16.
 */

class Type {

    private String typeName;
    private String thumbURL;

    @Override
    public String toString() {
        return "Type{" +
                "typeName='" + typeName + '\'' +
                ", thumbURL='" + thumbURL + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Type type = (Type) o;

        if (typeName != null ? !typeName.equals(type.typeName) : type.typeName != null)
            return false;
        return thumbURL != null ? thumbURL.equals(type.thumbURL) : type.thumbURL == null;

    }

    @Override
    public int hashCode() {
        int result = typeName != null ? typeName.hashCode() : 0;
        result = 31 * result + (thumbURL != null ? thumbURL.hashCode() : 0);
        return result;
    }

    public String getTypeName() {

        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getThumbURL() {
        return thumbURL;
    }

    public void setThumbURL(String thumbURL) {
        this.thumbURL = thumbURL;
    }

    public Type(String typeName, String thumbURL) {

        this.typeName = typeName;
        this.thumbURL = thumbURL;
    }
}
