package dev.vality.hooker.dao;

import dev.vality.hooker.AbstractIntegrationTest;
import dev.vality.hooker.dao.impl.CustomerDaoImpl;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.model.CustomerMessageEnum;
import dev.vality.hooker.model.EventType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

import static dev.vality.hooker.utils.BuildUtils.buildCustomerMessage;
import static org.junit.Assert.assertEquals;

/**
 * Created by inalarsanukaev on 09.04.17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CustomerMessageDaoImplTest extends AbstractIntegrationTest {
    private static Logger log = LoggerFactory.getLogger(CustomerMessageDaoImplTest.class);
    private static boolean messagesCreated = false;
    @Autowired
    CustomerDaoImpl messageDao;

    @Before
    public void setUp() throws Exception {
        if (!messagesCreated) {
            CustomerMessage message =
                    buildCustomerMessage(1L, "1234", EventType.CUSTOMER_CREATED, CustomerMessageEnum.CUSTOMER, "124",
                            "4356");
            message.setSequenceId(1L);
            message.setChangeId(1);
            messageDao.create(message);
            messagesCreated = true;
        }
    }

    @Test
    public void testDuplication() {
        CustomerMessage message =
                buildCustomerMessage(1L, "1234", EventType.CUSTOMER_CREATED, CustomerMessageEnum.CUSTOMER, "124",
                        "4356");
        message.setSequenceId(1L);
        message.setChangeId(1);
        messageDao.create(message);
        assertEquals(1, messageDao.getBy(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L)).size());
    }

    @Test
    public void get() throws Exception {
        CustomerMessage message = messageDao.getAny("124", CustomerMessageEnum.CUSTOMER);
        assertEquals(message.getShopId(), "4356");
    }

    @Test
    public void getMaxEventId() {
        assertEquals(messageDao.getMaxEventId().longValue(), 1L);
    }
}
