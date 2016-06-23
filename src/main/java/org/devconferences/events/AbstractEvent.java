package org.devconferences.events;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractEvent {
    public String id;
    public String name;
    public String url;
    public String description;
    public Boolean hidden;
    public final List<String> tags = new ArrayList<>();

    public AbstractEvent() {
        super();
    }

    public AbstractEvent(AbstractEvent obj) {
        super();
        id = obj.id;
        name = obj.name;
        url = obj.url;
        description = obj.description;
        tags.addAll(obj.tags);
        hidden = obj.hidden;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        AbstractEvent that = (AbstractEvent) o;

        if(id != null ? !id.equals(that.id) : that.id != null) return false;
        if(name != null ? !name.equals(that.name) : that.name != null) return false;
        if(url != null ? !url.equals(that.url) : that.url != null) return false;
        if(description != null ? !description.equals(that.description) : that.description != null) return false;
        if(hidden != null ? !hidden.equals(that.hidden) : that.hidden != null) return false;
        return tags.equals(that.tags);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (hidden != null ? hidden.hashCode() : 0);
        result = 31 * result + tags.hashCode();
        return result;
    }
}
