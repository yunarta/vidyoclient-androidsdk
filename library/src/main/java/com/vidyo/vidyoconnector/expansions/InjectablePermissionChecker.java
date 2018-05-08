package com.vidyo.vidyoconnector.expansions;

import android.content.Context;
import android.support.annotation.NonNull;

public interface InjectablePermissionChecker {

    int checkSelfPermission(@NonNull Context context, @NonNull String permission);
}
