package org.mytest.bindings;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.bindings.boot.BindingSpecificEnvironmentPostProcessor;

@SpringBootApplication
public class Main {
//    private final SpringApplication application = new SpringApplication();

    public static void main(String[] args) {
        System.setProperty("SERVICE_BINDING_ROOT", "/home/myeung/mygit/spring_sbo/spring-cloud-bindings/src/test/resources/k8s");
        SpringApplication.run(Main.class, args);

//        Main main = new Main();
//        main.test();
    }


    void test() {
        BindingSpecificEnvironmentPostProcessor be  = new BindingSpecificEnvironmentPostProcessor();

        System.out.println(be.getOrder());

    }
}
