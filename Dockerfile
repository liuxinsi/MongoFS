FROM daocloud.io/java:8u40

RUN apt-get update
RUN apt-get install -y openssh-server
RUN apt-get install -y fuse

RUN mkdir -p /var/run/sshd
RUN mkdir -p /root/.ssh

ADD key /root/.ssh/authorized_keys

ENTRYPOINT ["/usr/sbin/sshd","-D"]
EXPOSE 22

ADD target/mongofs-jar-with-dependencies.jar /root/mongofs.jar
ADD wait-for-it.sh /root/wait-for-it.sh
RUN chmod +x /root/wait-for-it.sh

ENTRYPOINT ["mkdir","-p","/root/mongfs_dir"]
ENTRYPOINT ["/root/wait-for-it.sh","mongodb:27017","-t","20","--","java","-jar","/root/mongofs.jar","-m","/root/mongfs_dir","-l","mongodb:27017","-d","test2"]
