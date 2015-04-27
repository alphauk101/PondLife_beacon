/********************************************************************
	created:	2014/08/27
	created:	27:8:2014   8:23
	filename: 	C:\Users\Lee_Laptop\Documents\GitHub\PondLife_beacon\ATtiny\MCUdriver\MCUdriver\MCUdriver.c
	file path:	C:\Users\Lee_Laptop\Documents\GitHub\PondLife_beacon\ATtiny\MCUdriver\MCUdriver
	file base:	MCUdriver
	file ext:	c
	author:	Lee Williams
	
	purpose:	To drive the water detection, using capicitance sensing.
*********************************************************************/


#include <avr/io.h>

int main(void)
{
	
	//On boot
	//Setup pins PA0 is our sensor pin.
	setup();
	
    while(1)
    {
       // Once we are up
    }
}

void setup(void)
{
	//Set Port A 0 to output and set it low so it sinks / Set up Port A 1 for pin toggle output 
	DDRA |= (1 << PA0) | (1 << PA1);
	PORTA &= ~((1 << PA0) & (1<< PA1));

}