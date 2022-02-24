package dev.vality.hooker.utils;

import dev.vality.damsel.payment_processing.EventRange;
import dev.vality.damsel.payment_processing.ServiceUser;
import dev.vality.damsel.payment_processing.UserInfo;
import dev.vality.damsel.payment_processing.UserType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HellgateUtils {

    public static final UserInfo USER_INFO = new UserInfo("hooker", UserType.service_user(new ServiceUser()));

    public static EventRange getEventRange(Integer limit) {
        return new EventRange().setLimit(limit);
    }

}
