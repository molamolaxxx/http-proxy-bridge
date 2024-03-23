# Http Proxy Bridge 
## Introduction

- http proxy bridge is a network framework based on netty
- It is a foundational framework model that can implement virtual network proxies, enabling a range of applications such as bypassing internet censorship and achieving network tunneling.
-  It combines reverse proxy and forward proxy techniques, along with SSL encryption technology, to make the access path more secure.

## Try It Simply

I have packaged a Docker image that includes the functionality to access private server ports, enabling scientific internet access.

```bash
docker pull molamolaxxx/http-proxy-encryption
docker run -p 22222:22222 http-proxy-encryption &
```

Then, we can attempt to access Google.com by using the HTTP proxy on port 22222. To switch the frontend proxy, you can use a proxy selector similar to SwitchyOmega.

## How To Build It

```bash
cd http-proxy-bridge
sh build.sh
```

To build this system, you will need two servers. One server should be located on a network that you can access (with a public IP), and the other server should be on the network you want to access (can be either a public or private IP, but IPv6 is not supported at the moment).

- **forward server**

We start foward server in server which has public ip

```bash
cd build
sh start_forward.sh
```

You need to modify the yml file and change the configuration to your private configuration

```yml
forward:
  servers:
    - port: 20434 # Proxy access port
      reversePort: 10434 # The reverse proxy will provide tcp connections to the forward proxy through this port
      type: SSL_HTTP # You need to start an http-proxy-encryption to connect 20434 port within this type
    - port: 20435
      reversePort: 10435
      type: HTTP # You can access the 20435 port directly
      openWhiteListsVerify: false # if this config is true,API will be called for whitelist verification
```

- **reverse server**

We start foward server in server which has public ip or private ip

```bash
cd build
sh start_reverse.sh
```

You need to modify the yml file and change the configuration to your private configuration\- 

```yml
reverse:
  servers:
    - remoteHost: 127.0.0.1 # forward server public ip
      remotePort: 10434 # forward server public reversePort
      channelNum: 128  # max tcp channels num 
      type: HTTP # default
  hostMapping: # If I visit molalocal.com, it maps it to localhost for me and accesses it through the reverse proxy server
    molalocal.com:80: localhost:80
    molalocal.com:6080: localhost:6080
```

- **encryption**

We start encryption  in your terminal

You need to modify the yml file and change the configuration to your private configuration

```yml
encrypt:
  servers:
    - remoteHost: 127.0.0.1 # forward server public ip
      remotePort: 20434  # forward server public port
      port: 22222
```

## Build With Docker

```bash
cd docker
sh build_docker.sh
# run your own container
sh run_docker.sh
```

