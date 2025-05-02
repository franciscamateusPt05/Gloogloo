package org.example.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FrontendApplication {

    @Bean
	public ServletRegistrationBean<ThymeleafServlet> thymeleafServletBean() {
		ServletRegistrationBean<ThymeleafServlet> bean = new ServletRegistrationBean<>(new ThymeleafServlet(), "/thymeleafServlet/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public ServletRegistrationBean<Example> exampleServletBean() {
		ServletRegistrationBean<Example> bean = new ServletRegistrationBean<>(new Example(), "/exampleServlet/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
    public IGateway gateway() throws Exception {
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        return (IGateway) registry.lookup("GatewayService");
	}

    public static void main(String[] args) {

		/*
		try {
            Main client = new Main();
            Properties prop = client.loadProperties();
            String host = prop.getProperty("rmi.host", "localhost");
            int port = Integer.parseInt(prop.getProperty("rmi.port", "1099"));
            String serviceName = prop.getProperty("rmi.service_name", "GatewayService");

            Registry registry = LocateRegistry.getRegistry(host, port);
            client.gateway = (IGateway) registry.lookup(serviceName);
            System.out.println("Connected to Gateway at rmi://" + host + ":" + port + "/" + serviceName);

        } catch (IOException | NotBoundException e) {
            System.err.println("Failed to connect to Gateway: " + e.getMessage());
        }
		*/

        SpringApplication.run(FrontendApplication.class, args);
    }

}
