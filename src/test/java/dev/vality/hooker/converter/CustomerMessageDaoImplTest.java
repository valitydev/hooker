package dev.vality.hooker.converter;

import dev.vality.damsel.proxy_provider.Intent;
import dev.vality.hooker.config.PostgresqlSpringBootITest;
import dev.vality.hooker.dao.impl.CustomerDaoImpl;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.model.CustomerMessageEnum;
import dev.vality.hooker.model.EventType;
import dev.vality.hooker.utils.BuildUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@PostgresqlSpringBootITest
public class CustomerMessageDaoImplTest {

    @Autowired
    private CustomerDaoImpl messageDao;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() throws Exception {
        CustomerMessage message =
                BuildUtils.buildCustomerMessage(1L, "1234", EventType.CUSTOMER_CREATED,
                        CustomerMessageEnum.CUSTOMER, "124",
                        "4356");
        message.setSequenceId(1L);
        message.setChangeId(1);
        messageDao.save(message);
    }

    @Test
    public void testDuplication() {
        CustomerMessage message =
                BuildUtils.buildCustomerMessage(1L, "1234", EventType.CUSTOMER_CREATED,
                        CustomerMessageEnum.CUSTOMER, "124",
                        "4356");
        message.setSequenceId(1L);
        message.setChangeId(1);
        messageDao.save(message);
        assertEquals(1,
                jdbcTemplate.queryForList("select 1 from hook.customer_message", Map.of(), Integer.class).size());
    }

    @Test
    public void get() throws Exception {
        CustomerMessage message = messageDao.getAny("124", CustomerMessageEnum.CUSTOMER);
        assertEquals(message.getShopId(), "4356");
    }

}
