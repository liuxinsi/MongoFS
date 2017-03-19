package com.lxs.mongofs;

import com.mongodb.MongoClient;
import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

/**
 * @author liuxinsi
 * @mail akalxs@gmail.com
 */
public class MongoAccessorTest {
    private MongoAccessor accessor;

    @BeforeClass
    public void before() {
        MongoClient mc = new MongoClient("127.0.0.1", 27017);
        accessor = new MongoAccessor(mc, "test");
    }

    @Test
    public void testSave() {
        byte[] data = null;
        try {
            data = IOUtils.toByteArray(
                    MongoAccessorTest.class.getResourceAsStream("../../../test.jpg")
            );
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        String name = Long.toString(System.currentTimeMillis());
        String id = accessor.save(data, "/" + name);
        Assert.assertNotNull(id);
    }

    @Test
    public void testLoadFileNames() {
        List<String> nameList = accessor.loadFileNames("/");
        Assert.assertTrue(!nameList.isEmpty());
    }
}
