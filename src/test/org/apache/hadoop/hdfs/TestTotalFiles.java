package org.apache.hadoop.hdfs;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class TestTotalFiles {
  private static MiniDFSCluster cluster;
  private static Configuration conf;
  private static FileSystem fs;
  private static final int BLOCK_SIZE = 1024;
  private static final int MAX_BLOCKS = 10;
  private static final int MAX_FILE_SIZE = MAX_BLOCKS * BLOCK_SIZE;
  private static final Random random = new Random();
  private static final Log LOG = LogFactory.getLog(TestTotalFiles.class);

  @Before
  public void setUp() throws Exception {
    conf = new Configuration();
    conf.setInt("dfs.block.size", BLOCK_SIZE);
    cluster = new MiniDFSCluster(conf, 3, true, null);
    fs = cluster.getFileSystem();
  }

  @After
  public void tearDown() throws Exception {
    cluster.shutdown();
  }

  @Test
  public void testBasic() throws Exception {
    String topDir = "testBasic";
    DFSTestUtil util = new DFSTestUtil(topDir, 100, 10, MAX_FILE_SIZE);
    util.createFiles(fs, topDir);
    FSNamesystem namesystem = cluster.getNameNode().namesystem;
    assertEquals(100, namesystem.getFilesTotal());
    assertTrue(namesystem.getFilesAndDirectoriesTotal() > namesystem
        .getFilesTotal());
  }

  @Test
  public void testRestart() throws Exception {
    String topDir = "testRestart";
    DFSTestUtil util = new DFSTestUtil(topDir, 100, 10, MAX_FILE_SIZE);
    util.createFiles(fs, topDir);
    FSNamesystem namesystem = cluster.getNameNode().namesystem;
    assertEquals(100, namesystem.getFilesTotal());
    assertTrue(namesystem.getFilesAndDirectoriesTotal() > namesystem
        .getFilesTotal());

    cluster.restartNameNodes();
    namesystem = cluster.getNameNode().namesystem;
    assertEquals(100, namesystem.getFilesTotal());
    assertTrue(namesystem.getFilesAndDirectoriesTotal() > namesystem
        .getFilesTotal());
  }

  private int deleteFiles(DFSTestUtil util, String topDir) throws Exception {
    int deleted = 0;
    for (String fileName : util.getFileNames(topDir)) {
      if (random.nextBoolean()) {
        cluster.getNameNode().delete(fileName, false);
        deleted++;
      }
    }
    return deleted;
  }

  private int concatFiles(DFSTestUtil util, String topDir) throws Exception {
    String files[] = util.getFileNames(topDir);
    int index = random.nextInt(files.length - 1);
    String target = files[index];
    String[] srcs = Arrays.copyOfRange(files, index + 1, files.length);
    cluster.getNameNode().concat(target, srcs, false);
    return srcs.length;
  }

  @Test
  public void testRestartWithSaveNamespace() throws Exception {
    String topDir = "/testRestartWithSaveNamespace";
    FSNamesystem namesystem = null;
    int totalFiles = 0;
    for (int i = 0; i < 10; i++) {
      DFSTestUtil util = new DFSTestUtil(topDir, 5, 10, MAX_FILE_SIZE);
      util.createFiles(fs, topDir);
      DFSTestUtil util1 = new DFSTestUtil(topDir, 5, 1, MAX_FILE_SIZE);
      util1.createFiles(fs, topDir);
      totalFiles += 10;
      totalFiles -= deleteFiles(util, topDir);
      totalFiles -= concatFiles(util1, topDir);
      if (random.nextBoolean()) {
        cluster.getNameNode().saveNamespace(true, false);
      }
      namesystem = cluster.getNameNode().namesystem;
      assertEquals(totalFiles, namesystem.getFilesTotal());
      cluster.restartNameNodes();
      namesystem = cluster.getNameNode().namesystem;
      assertEquals(totalFiles, namesystem.getFilesTotal());
    }

    assertTrue(namesystem.getFilesAndDirectoriesTotal() > namesystem
        .getFilesTotal());
  }

  @Test
  public void testDeletes() throws Exception {
    String topDir = "testDeletes";
    DFSTestUtil util = new DFSTestUtil(topDir, 100, 10, MAX_FILE_SIZE);
    util.createFiles(fs, topDir);
    FSNamesystem namesystem = cluster.getNameNode().namesystem;
    assertEquals(100, namesystem.getFilesTotal());
    assertTrue(namesystem.getFilesAndDirectoriesTotal() > namesystem
        .getFilesTotal());
    int deleted = 0;
    for (String fileName : util.getFileNames(topDir)) {
      if (random.nextBoolean()) {
        deleted++;
        fs.delete(new Path(fileName), false);
      }
    }
    assertEquals(100 - deleted, namesystem.getFilesTotal());
  }

  @Test
  public void testConcat() throws Exception {
    String topDir = "/testConcat";
    DFSTestUtil util = new DFSTestUtil(topDir, 100, 1, MAX_FILE_SIZE);
    util.createFiles(fs, topDir);
    FSNamesystem namesystem = cluster.getNameNode().namesystem;
    assertEquals(100, namesystem.getFilesTotal());
    assertTrue(namesystem.getFilesAndDirectoriesTotal() > namesystem
        .getFilesTotal());
    String[] files = util.getFileNames(topDir);
    for (int i = 0; i < files.length; i += 10) {
      String target = files[i];
      String[] srcs = Arrays.copyOfRange(files, i + 1, i + 10);
      cluster.getNameNode().concat(target, srcs, false);
    }
    assertEquals(10, namesystem.getFilesTotal());
  }
}