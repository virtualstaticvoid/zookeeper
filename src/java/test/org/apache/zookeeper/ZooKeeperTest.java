/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.zookeeper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.zookeeper.AsyncCallback.VoidCallback;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.test.ClientBase;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * Testing Zookeeper public methods
 *
 */
public class ZooKeeperTest extends ClientBase {

    @Test
    public void testDeleteRecursive() throws IOException, InterruptedException,
            KeeperException {
        final ZooKeeper zk = createClient();
        // making sure setdata works on /
        zk.setData("/", "some".getBytes(), -1);
        zk.create("/a", "some".getBytes(), Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);

        zk.create("/a/b", "some".getBytes(), Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);

        zk.create("/a/b/v", "some".getBytes(), Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);

        zk.create("/a/b/v/1", "some".getBytes(), Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);

        zk.create("/a/c", "some".getBytes(), Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);

        zk.create("/a/c/v", "some".getBytes(), Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);

        List<String> children = zk.getChildren("/a", false);

        Assert.assertEquals("2 children - b & c should be present ", children
                .size(), 2);
        Assert.assertTrue(children.contains("b"));
        Assert.assertTrue(children.contains("c"));

        ZKUtil.deleteRecursive(zk, "/a");
        Assert.assertNull(zk.exists("/a", null));
    }

    @Test
    public void testDeleteRecursiveAsync() throws IOException,
            InterruptedException, KeeperException {
        final ZooKeeper zk = createClient();
        // making sure setdata works on /
        zk.setData("/", "some".getBytes(), -1);
        zk.create("/a", "some".getBytes(), Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);

        zk.create("/a/b", "some".getBytes(), Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);

        zk.create("/a/b/v", "some".getBytes(), Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);

        zk.create("/a/b/v/1", "some".getBytes(), Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);

        zk.create("/a/c", "some".getBytes(), Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);

        zk.create("/a/c/v", "some".getBytes(), Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);

        for (int i = 0; i < 50; ++i) {
            zk.create("/a/c/" + i, "some".getBytes(), Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT);
        }
        List<String> children = zk.getChildren("/a", false);

        Assert.assertEquals("2 children - b & c should be present ", children
                .size(), 2);
        Assert.assertTrue(children.contains("b"));
        Assert.assertTrue(children.contains("c"));

        VoidCallback cb = new VoidCallback() {

            @Override
            public void processResult(int rc, String path, Object ctx) {
                synchronized (ctx) {
                    ((AtomicInteger) ctx).set(4);
                    ctx.notify();
                }
            }

        };
        final AtomicInteger ctx = new AtomicInteger(3);
        ZKUtil.deleteRecursive(zk, "/a", cb, ctx);
        synchronized (ctx) {
            ctx.wait();
        }
        Assert.assertEquals(4, ((AtomicInteger) ctx).get());
    }
    
    @Test
    public void testStatWhenPathDoesNotExist() throws IOException,
    		InterruptedException {
    	final ZooKeeper zk = createClient();
    	ZooKeeperMain main = new ZooKeeperMain(zk);
    	String cmdstring = "stat /invalidPath";
    	main.cl.parseCommand(cmdstring);
    	try {
    		main.processZKCmd(main.cl);
    		Assert.fail("As Node does not exist, command should fail by throwing No Node Exception.");
    	} catch (KeeperException e) {
    		Assert.assertEquals("KeeperErrorCode = NoNode for /invalidPath", e.getMessage());
    	}
    }

    @Test
    public void testParseWithExtraSpaces() throws Exception {
        final ZooKeeper zk = createClient();
        ZooKeeperMain zkMain = new ZooKeeperMain(zk);
        String cmdstring = "      ls       /  ";
        zkMain.cl.parseCommand(cmdstring);
        Assert.assertEquals("Spaces also considered as characters", zkMain.cl.getNumArguments(), 2);
        Assert.assertEquals("ls is not taken as first argument", zkMain.cl.getCmdArgument(0), "ls");
        Assert.assertEquals("/ is not taken as second argument", zkMain.cl.getCmdArgument(1), "/");
    }

    @Test
    public void testInvalidCommand() throws Exception {
        final ZooKeeper zk = createClient();
        ZooKeeperMain zkMain = new ZooKeeperMain(zk);
        String cmdstring = "cret -s /node1";
        zkMain.cl.parseCommand(cmdstring);
        Assert.assertFalse("Doesn't validate the command", zkMain
                .processZKCmd(zkMain.cl));
    }

    @Test
    public void testCreateCommandWithoutPath() throws Exception {
        final ZooKeeper zk = createClient();
        ZooKeeperMain zkMain = new ZooKeeperMain(zk);
        String cmdstring = "create ";
        zkMain.cl.parseCommand(cmdstring);
        Assert.assertFalse("Path is not validated.", zkMain
                .processZKCmd(zkMain.cl));
        // create ephemeral
        cmdstring = "create -e ";
        zkMain.cl.parseCommand(cmdstring);
        Assert.assertFalse("Path is not validated.", zkMain
                .processZKCmd(zkMain.cl));
        // create sequential
        cmdstring = "create -s ";
        zkMain.cl.parseCommand(cmdstring);
        Assert.assertFalse("Path is not validated.", zkMain
                .processZKCmd(zkMain.cl));
        // create ephemeral sequential
        cmdstring = "create -s -e ";
        zkMain.cl.parseCommand(cmdstring);
        Assert.assertFalse("Path is not validated.", zkMain
                .processZKCmd(zkMain.cl));
    }

    @Test
    public void testCreateNodeWithoutData() throws Exception {
        final ZooKeeper zk = createClient();
        ZooKeeperMain zkMain = new ZooKeeperMain(zk);
        // create persistent sequential node
        String cmdstring = "create -s /node ";
        zkMain.cl.parseCommand(cmdstring);
        Assert.assertTrue("Doesn't create node without data", zkMain
                .processZKCmd(zkMain.cl));
        // create ephemeral node
        cmdstring = "create  -e /node ";
        zkMain.cl.parseCommand(cmdstring);
        Assert.assertTrue("Doesn't create node without data", zkMain
                .processZKCmd(zkMain.cl));
        // create ephemeral sequential node
        cmdstring = "create -s -e /node ";
        zkMain.cl.parseCommand(cmdstring);
        Assert.assertTrue("Doesn't create node without data", zkMain
                .processZKCmd(zkMain.cl));
        // creating ephemeral with wrong option.
        cmdstring = "create -s y /node";
        zkMain.cl.parseCommand(cmdstring);
        try {
            Assert.assertTrue("Created node with wrong option", zkMain
                    .processZKCmd(zkMain.cl));
            Assert.fail("Created the node with wrong option should "
                    + "throw Exception.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Path must start with / character", e
                    .getMessage());
        }
    }

    @Test
    public void testACLWithExtraAgruments() throws Exception {
        final ZooKeeper zk = createClient();
        ZooKeeperMain zkMain = new ZooKeeperMain(zk);
        // create persistent sequential node
        String cmdstring = "create -s /l data ip:10.18.52.144:cdrwa f g h";
        zkMain.cl.parseCommand(cmdstring);
        Assert.assertTrue(
                "Not considering the extra arguments after the acls.", zkMain
                        .processZKCmd(zkMain.cl));
    }

    @Test
    public void testCreatePersistentNode() throws Exception {
        final ZooKeeper zk = createClient();
        ZooKeeperMain zkMain = new ZooKeeperMain(zk);
        String cmdstring = "create /node2";
        zkMain.cl.parseCommand(cmdstring);
        Assert.assertTrue("Not creating Persistent node.", zkMain
                .processZKCmd(zkMain.cl));
    }

    @Test
    public void testDelete() throws Exception {
        final ZooKeeper zk = createClient();
        ZooKeeperMain zkMain = new ZooKeeperMain(zk);
        String cmdstring1 = "create -e /node2 data";
        String cmdstring2 = "delete /node2";
        String cmdstring3 = "ls /node2";
        zkMain.cl.parseCommand(cmdstring1);
        Assert.assertTrue(zkMain.processZKCmd(zkMain.cl));
        zkMain.cl.parseCommand(cmdstring2);
        Assert.assertFalse(zkMain.processZKCmd(zkMain.cl));
        zkMain.cl.parseCommand(cmdstring3);
        Assert.assertFalse("", zkMain.processCmd(zkMain.cl));
    }

    @Test
    public void testStatCommand() throws Exception {
        final ZooKeeper zk = createClient();
        ZooKeeperMain zkMain = new ZooKeeperMain(zk);
        String cmdstring1 = "create -e /node3 data";
        String cmdstring2 = "stat /node3";
        String cmdstring3 = "delete /node3";
        zkMain.cl.parseCommand(cmdstring1);
        Assert.assertTrue(zkMain.processZKCmd(zkMain.cl));
        zkMain.cl.parseCommand(cmdstring2);
        Assert.assertFalse(zkMain.processZKCmd(zkMain.cl));
        zkMain.cl.parseCommand(cmdstring3);
        Assert.assertFalse(zkMain.processZKCmd(zkMain.cl));
    }

    @Test
    public void testInvalidStatCommand() throws Exception {
        final ZooKeeper zk = createClient();
        ZooKeeperMain zkMain = new ZooKeeperMain(zk);
        // node doesn't exists
        String cmdstring1 = "stat /node123";
        zkMain.cl.parseCommand(cmdstring1);
        try {
            Assert.assertFalse(zkMain.processZKCmd(zkMain.cl));
            Assert.fail("Path doesn't exists so, command should fail.");
        } catch (KeeperException e) {
            Assert.assertEquals(KeeperException.Code.NONODE, e.code());
        }
    }

    @Test
    public void testSetData() throws Exception {
        final ZooKeeper zk = createClient();
        ZooKeeperMain zkMain = new ZooKeeperMain(zk);
        String cmdstring1 = "create -e /node4 data";
        String cmdstring2 = "set /node4 " + "data";
        String cmdstring3 = "delete /node4";
        Stat stat = new Stat();
        int version = 0;
        zkMain.cl.parseCommand(cmdstring1);
        Assert.assertTrue(zkMain.processZKCmd(zkMain.cl));
        stat = zk.exists("/node4", true);
        version = stat.getVersion();
        zkMain.cl.parseCommand(cmdstring2);
        Assert.assertTrue(zkMain.processZKCmd(zkMain.cl));
        stat = zk.exists("/node4", true);
        Assert.assertEquals(version + 1, stat.getVersion());
        zkMain.cl.parseCommand(cmdstring3);
        Assert.assertFalse(zkMain.processZKCmd(zkMain.cl));
    }
}
