package dev.khanh.mcp.dejaredmcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the DeJared MCP server.
 *
 * <p>A Spring Boot application that exposes JAR analysis tools (package discovery,
 * class search, bytecode metadata extraction, and multi-engine decompilation) via
 * the Model Context Protocol (MCP) over STDIO transport.
 */
@SpringBootApplication
public class DejaredMcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(DejaredMcpApplication.class, args);
	}

}
