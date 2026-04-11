package org.example.bill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BillApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillApplication.class, args);
        System.out.println("项目启动成功");
    }

}
