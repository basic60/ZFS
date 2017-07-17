using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;

namespace Monitor
{
    /// <summary>
    /// MainWindow.xaml 的交互逻辑
    /// </summary>
    public partial class MainWindow : Window
    {

        public ObservableCollection<StNodeInfo> nodeInfo;
        public ObservableCollection<FileInfo> fileInfo;
        Thread nodeInfoListenTask;
        Thread fileInfoListenerTask;

        public MainWindow()
        {
            InitializeComponent();
            nodeInfo = new ObservableCollection<StNodeInfo>();
            fileInfo = new ObservableCollection<FileInfo>();
            nodeList.ItemsSource = nodeInfo;
            fileList.ItemsSource = fileInfo;

         
            nodeInfoListenTask = new Thread(nodeInfoListener); nodeInfoListenTask.IsBackground = true;
            nodeInfoListenTask.Start();

            fileInfoListenerTask = new Thread(fileInfoListener); fileInfoListenerTask.IsBackground = true;
            fileInfoListenerTask.Start();
        }

        #region StorageNodeInfoListener
        string[] nodeArr;
        void updateNode()
        {
            nodeInfo.Clear(); StNodeInfo tmp;
            int i = 0;
            while (i != nodeArr.Length - 1)
            {
                tmp = new StNodeInfo();
                tmp.nodeName = nodeArr[i++];
                tmp.host = nodeArr[i++];
                tmp.availaleBytes = long.Parse(nodeArr[i++]);
                tmp.totalBytes = long.Parse(nodeArr[i++]);
                nodeInfo.Add(tmp);
            }
        }
        void nodeInfoListener()
        {
            const int LISTEN_PORT = 35000;
            IPAddress ip = IPAddress.Parse("127.0.0.1");
            IPEndPoint local = new IPEndPoint(ip, LISTEN_PORT);
            UdpClient client = new UdpClient(local);

            while (true)
            {
                try
                {
                    byte[] rec = client.Receive(ref local);
                    string msg = Encoding.UTF8.GetString(rec, 0, rec.Length);
                    nodeArr = msg.Split('\n');
                    this.nodeList.Dispatcher.Invoke(updateNode);
                }
                catch (Exception e)
                {
                    Console.WriteLine(e.Message);
                }
            }
        }
        #endregion

        #region FileInfoListener
        string[] fileArr;

        void updateFile()
        {
            fileInfo.Clear();FileInfo tmp;
            int i = 0;
            while (i != fileArr.Length - 1)
            {
                tmp = new FileInfo();
                tmp.fileName = fileArr[i++];
                tmp.uuid = fileArr[i++];
                tmp.initSize = long.Parse(fileArr[i++]);
                tmp.finalSize = long.Parse(fileArr[i++]);
                tmp.mainNode = fileArr[i ++];
                tmp.backupNode = fileArr[i ++];
                fileInfo.Add(tmp);
            }
        }

        void fileInfoListener()
        {
            const int LISTEN_PORT = 35001;
            IPAddress ip = IPAddress.Parse("127.0.0.1");
            IPEndPoint local = new IPEndPoint(ip, LISTEN_PORT);
            UdpClient client = new UdpClient(local);

            while (true)
            {
                try
                {
                    byte[] rec = client.Receive(ref local);
                    string msg = Encoding.UTF8.GetString(rec, 0, rec.Length);
                    fileArr = msg.Split('\n');
                    this.fileList.Dispatcher.Invoke(updateFile);
                }
                catch (Exception e)
                {
                    Console.WriteLine(e.Message);
                }
            }
        }
        #endregion
    }

    public class StNodeInfo
    {
        public String nodeName { get; set; }
        public String host { get; set; }
        public long availaleBytes { get; set; }
        public long totalBytes { get; set; }
    }

    public class FileInfo
    {
        public String fileName { get; set; }
        public String uuid { get; set; }
        public long initSize { get; set; }
        public long finalSize { get; set; }
        public String mainNode { get; set; }
        public String backupNode { get; set; }
    }
}
