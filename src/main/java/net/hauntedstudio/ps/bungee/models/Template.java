package net.hauntedstudio.ps.bungee.models;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.List;

@Getter
@Setter
public class Template {
    private String id;
    private String name;
    private String description;
    private String version;
    private boolean doesNeedPermissions;
    private List<String> tags = List.of();

    private transient String path;
    private transient File serverFile;
    private transient File serverPropertiesFile;

    public boolean doesNeedPermissions() {
        return doesNeedPermissions;
    }

}
