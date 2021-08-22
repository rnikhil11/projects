import matplotlib.pyplot as plt
import numpy as np
import os
import sys

inputFile = os.path.join(sys.path[0], sys.argv[1])
fileReader = open(inputFile, "r", encoding="utf-8", errors='ignore')

lines = fileReader.readlines()
entries = {}
for line in lines:
    n, d, c, rt, thru, msg = line.split(" ")
    n = int(n)
    d = int(d)
    c = int(c)
    rt = int(rt)
    thru = float(thru)
    msg = int(msg)
    entries[(n, d, c)] = (rt, thru, msg)

x_points_c = np.array([10, 20, 30, 40, 50])

# for _n in [3]:
#     for _d in [10]:
#         fig, ax1 = plt.subplots()
#         ax2 = ax1.twinx()

#         y_points_thru = []
#         y_points_rt = []
#         for _c in x_points_c:
#             y_points_thru.append(entries[(_n, _d, _c)][1])
#             y_points_rt.append(entries[(_n, _d, _c)][0])

#         ax1.plot(x_points_c, np.array(y_points_thru), 'g-')
#         ax2.plot(x_points_c, np.array(y_points_rt), 'b-')

#         ax1.set_xlabel('CS Exec time')
#         ax1.set_ylabel('System throughput', color='g')
#         ax2.set_ylabel('Avg Response time', color='b')
#         fig.suptitle('n='+str(_n)+', d='+str(_d), fontsize=20)

#         plt.show()
#         fig.savefig('n'+str(_n)+'-'+'d'+str(_d)+'.jpg')

# x_points_n = np.array([1, 3, 5, 7, 9])
# for _c in [10, 20, 30, 40, 50]:
#     for _d in [10, 15, 20, 25]:
#         fig, ax1 = plt.subplots()
#         ax2 = ax1.twinx()

#         y_points_thru = []
#         y_points_rt = []
#         for _n in x_points_n:
#             y_points_thru.append(entries[(_n, _d, _c)][1])
#             y_points_rt.append(entries[(_n, _d, _c)][0])

#         ax1.plot(x_points_n, np.array(y_points_thru), 'g-')
#         ax2.plot(x_points_n, np.array(y_points_rt), 'b-')

#         ax1.set_xlabel('Num of nodes')
#         ax1.set_ylabel('System throughput', color='g')
#         ax2.set_ylabel('Avg Response time', color='b')
#         fig.suptitle('c='+str(_c)+', d='+str(_d), fontsize=20)

#         plt.show()
#         fig.savefig('c'+str(_c)+'-'+'d'+str(_d)+'.jpg')

x_points_d = np.array([10, 15, 20, 25])
for _c in [10, 20, 30, 40, 50]:
    for _n in [1,3,5,7,9]:
        fig, ax1 = plt.subplots()
        ax2 = ax1.twinx()

        y_points_thru = []
        y_points_rt = []
        for _d in x_points_d:
            y_points_thru.append(entries[(_n, _d, _c)][1])
            y_points_rt.append(entries[(_n, _d, _c)][0])

        ax1.plot(x_points_d, np.array(y_points_thru), 'g-')
        ax2.plot(x_points_d, np.array(y_points_rt), 'b-')

        ax1.set_xlabel('Inter request delay')
        ax1.set_ylabel('System throughput', color='g')
        ax2.set_ylabel('Avg Response time', color='b')
        fig.suptitle('c='+str(_c)+', n='+str(_n), fontsize=20)

        plt.show()
        fig.savefig('c'+str(_c)+'-'+'n'+str(_n)+'.jpg')
