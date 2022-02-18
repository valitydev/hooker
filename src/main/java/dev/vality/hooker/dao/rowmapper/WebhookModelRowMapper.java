package dev.vality.hooker.dao.rowmapper;

import dev.vality.hooker.model.Message;
import dev.vality.hooker.model.WebhookMessageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

@RequiredArgsConstructor
public class WebhookModelRowMapper<T extends Message> implements RowMapper<WebhookMessageModel<T>> {

    public static final String HOOK_ID = "hook_id";
    public static final String URL = "url";
    public static final String PRIV_KEY = "priv_key";
    private final RowMapper<T> messageRowMapper;

    @Override
    public WebhookMessageModel<T> mapRow(ResultSet rs, int rowNum) throws SQLException {
        T invoicingMessage = messageRowMapper.mapRow(rs, rowNum);
        WebhookMessageModel<T> webhookMessageModel = new WebhookMessageModel<>();
        webhookMessageModel.setMessage(invoicingMessage);
        webhookMessageModel.setHookId(rs.getLong(HOOK_ID));
        webhookMessageModel.setUrl(rs.getString(URL));
        webhookMessageModel.setPrivateKey(rs.getString(PRIV_KEY));
        return webhookMessageModel;
    }
}
