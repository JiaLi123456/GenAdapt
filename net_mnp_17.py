#!/usr/bin/python

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import RemoteController
from mininet.node import Node
from mininet.node import CPULimitedHost
from mininet.link import TCLink
from mininet.cli import CLI
from mininet.log import setLogLevel
from mininet.util import dumpNodeConnections

class FullTopo( Topo ):

    def addSwitch( self, name, **opts ):
        kwargs = { 'protocols' : 'OpenFlow13' }
        kwargs.update( opts )
        return super(FullTopo, self).addSwitch( name, **kwargs )

    def __init__( self ):
        "Create a topology."
        SIZE = 17

        Topo.__init__( self )

        # switches: MUST s1..sn
        s = []
        for i in range(SIZE):
            s.append( self.addSwitch( 's' + str(i+1) ) )

        # hosts: MUST h1..sm
        h = []
        for i in range(17):
            h.append( self.addHost( 'h' + str(i+1) ) )


                # links between switch and corresponding host
        for i in range(16):
            self.addLink( s[0], h[i] )

        self.addLink( s[1], h[16] )


        self.addLink( s[0], s[1], bw=100, delay='25ms' )
        self.addLink( s[0], s[2], bw=100, delay='25ms' )
        self.addLink( s[2], s[1], bw=100, delay='25ms' )
        self.addLink( s[0], s[3], bw=100, delay='25ms' )
        self.addLink( s[3], s[4], bw=100, delay='25ms' )
        self.addLink( s[4], s[1], bw=100, delay='25ms' )
        self.addLink( s[0], s[5], bw=100, delay='25ms' )
        self.addLink( s[5], s[6], bw=100, delay='25ms' )
        self.addLink( s[6], s[7], bw=100, delay='25ms' )
        self.addLink( s[7], s[1], bw=100, delay='25ms' )
        self.addLink( s[0], s[8], bw=100, delay='25ms' )
        self.addLink( s[8], s[9], bw=100, delay='25ms' )
        self.addLink( s[9], s[10], bw=100, delay='25ms' )
        self.addLink( s[10], s[11], bw=100, delay='25ms' )
        self.addLink( s[11], s[1], bw=100, delay='25ms' )
        self.addLink( s[0], s[12], bw=100, delay='25ms' )
        self.addLink( s[12], s[13], bw=100, delay='25ms' )
        self.addLink( s[13], s[14], bw=100, delay='25ms' )
        self.addLink( s[14], s[15], bw=100, delay='25ms' )
        self.addLink( s[15], s[16], bw=100, delay='25ms' )
        self.addLink( s[16], s[1], bw=100, delay='25ms' )

topos = { 'full': ( lambda: FullTopo() ) }

if __name__ == '__main__':
    from onosnetssh import run
    run( FullTopo() )
