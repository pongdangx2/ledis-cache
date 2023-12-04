package me.lkh.lediscache.test.util;

import me.lkh.lediscache.test.domain.TestDomain;
import org.springframework.stereotype.Component;

@Component
public class TestUtil {

    public String pringString(TestDomain testDomain){
        return testDomain.getStr() + ":lkhtest";
    }
}
