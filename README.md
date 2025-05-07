# Engelen's CICFlowMeter with PCAP Indexes

This is a modified version of CICFlowMeter tool from [pull request #11](https://github.com/GintsEngelen/CICFlowMeter/pull/11). Other than generating flow CSV, this tool also generates the PCAP indexes of every packet contained in the flow. For example, if a flow has PCAP indexes 10, 11, and 13, that means the 10th, 11th, and 13th packets in the PCAP are part of that flow. **Note that at this point, it only supports file inputs, not folder inputs.**

## Running the Tool

```bash
# Building
sudo mvn clean install -Djava.library.path="/path/to/project/jnetpcap/linux/jnetpcap-1.4.r1500"

# Running
sudo bash
cd target
java -Djava.library.path="/path/to/project/jnetpcap/linux/jnetpcap-1.4.r1500" -jar CICFlowMeterV3-0.0.4-SNAPSHOT.jar <pcap-path> <output-folder>
```

## Original README

As part of our [WTMC 2021 paper](https://downloads.distrinet-research.be/WTMC2021/Resources/wtmc2021_Engelen_Troubleshooting.pdf), we analysed and improved the CICFlowMeter tool, the result of
which can be found in this repository. If you use this improved CICFlowMeter tool, please cite our paper:

            @inproceedings{engelen2021troubleshooting,
            title={Troubleshooting an Intrusion Detection Dataset: the CICIDS2017 Case Study},
            author={Engelen, Gints and Rimmer, Vera and Joosen, Wouter},
            booktitle={2021 IEEE Security and Privacy Workshops (SPW)},
            pages={7--12},
            year={2021},
            organization={IEEE}
            }

A detailed list of all fixes and improvements, as well as implications of the changes can be found on [our webpage](https://downloads.distrinet-research.be/WTMC2021/),
which hosts the extended documentation of our paper.

Here we stick to a brief summary of all changes to the CICFlowMeter tool:

- A TCP flow is no longer terminated after a single FIN packet. It now terminates after mutual exchange of
  FIN packets, which is more in line with the TCP specification.
- An RST packet is no longer ignored. Instead, the RST packet also terminates a TCP flow.

- The Flow Active and Idle time features no longer encode an absolute timestamp.

- The values for _Fwd PSH Flags_, _Bwd PSH Flags_, _Fwd URG Flags_ and _Bwd URG Flags_ are now correctly incremented.
