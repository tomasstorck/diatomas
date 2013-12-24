% [fn, pn] = uigetfile('*.*');

%%%%%%%%
close all
fn = 'test';
pn = '/home/tomas/Desktop/';
%%%%%%%%%

fid = fopen([pn fn],'r');
line = '';
while true
	line = fgetl(fid);
	try 
		str = regexprep(line,'.\t.','.=.','once');
	catch ME
	end
		
end
	
fclose(fid);