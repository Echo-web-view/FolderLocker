package com.folderlocker.platform;

import java.io.IOException;

public interface FolderAccessController {
    void lock(String folder) throws IOException;

    void unlock(String folder) throws IOException;
}
