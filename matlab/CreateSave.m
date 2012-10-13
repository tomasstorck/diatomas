function CreateSave

clc

fid=fopen('../src/cell/CModel.java');
c = '';

% Make header
fprintf('\tpublic static void Save() {\n');
fprintf('\t\tMLStructure mlModel = new MLStructure("model", new int[] {1,1});\n');
fprintf('\t\tint N;\n')		% we'll need this one for the arrays
fprintf('\t\tdouble[] arrayIndex;\n')	% we'll need this one for the arrays' arrays
		

while isempty(strfind(c,'//////////////////////////////////'))	% while we don't run into end of section (= lot of slashes)
	c=fgetl(fid);
	
	% Ignore section
	if isempty(strtrim(c)) || ...
		~isempty(strfind(c,'package')) || ...
		~isempty(strfind(c,'import')) || ...
		~isempty(strfind(c,'public class')) || ...
		~isempty(strfind(c,'//////////////////////////////////'))
			continue
	end
	
	% Operate on the line
	% comment
	ctrim = strtrim(c);
	if length(ctrim)>2 && strcmp(ctrim(1:2),'//') && ~strcmp(ctrim(1:3),'///')
		fprintf(['\t\t' ctrim '\n']);
		continue
	end
	% double
	if hasstr(c,'double ')
		[n comment] = splitline(c,'double');
		fprintf('\t\t%-50s%-80s\t%s\n',['mlModel.setField("' n '",'],['new MLDouble(null, new double[] {' n '}, 1));'],comment);
		continue
	end
	% double[]
	if hasstr(c,'double[] ')
		[n comment] = splitline(c,'double\[\]');
		fprintf('\t\t%-50s%-80s\t%s\n',['mlModel.setField("' n '",'],['new MLDouble(null, ' n ', ' n '.length));'], comment);
		continue
	end
	% int
	if hasstr(c,'int ')
		[n comment] = splitline(c,'int');
		fprintf('\t\t%-50s%-80s\t%s\n',['mlModel.setField("' n '",'],['new MLDouble(null, new double[] {' n '}, 1));'],comment);
		continue
	end
	% int[]					// Ugly one as we can't cast int[] --> double[]
	if hasstr(c,'int[] ')
		[n comment] = splitline(c,'int\[\]');
		fprintf('\t\t//\n');
		fprintf('\t\tdouble[] D%s = new double[%s.length];',n,n);
		fprintf('\t\tfor(int ii=0; ii<%s.length; ii++)\t\tD%s[ii] = %s[ii];',n,n,n);
		fprintf('\t\t%-50s%-80s\t%s\n',['mlModel.setField("' n '",'],['new MLDouble(null, D' n ', ' n '.length));'], comment);
		fprintf('\t\t//\n');
		continue
	end
	% Vector3d
	if hasstr(c,'Vector3d ')
		[n comment] = splitline(c,'Vector3d');
		fprintf('\t\t%-50s%-80s\t%s\n',['mlModel.setField("' n '",'],['new MLDouble(null, new double[] {' n '.x, ' n '.y, ' n '.z}, 3));'], comment);
		continue
	end
	% String
	if hasstr(c,'String ')
		[n comment] = splitline(c,'String');
		fprintf('\t\t%-50s%-80s\t%s\n',['mlModel.setField("' n '",'],['new MLChar(null, new String[] {' n '}, 1));'], comment);
		continue
	end
	% String[]
	if hasstr(c,'String[] ')
		[n comment] = splitline(c,'String\[\]');
		fprintf('\t\t%-50s%-80s\t%s\n',['mlModel.setField("' n '",'],['new MLChar(null, ' n '));'], comment);
		continue
	end
	% boolean
	if hasstr(c,'boolean ')
		[n comment] = splitline(c,'boolean');
		fprintf('\t\t%-50s%-80s\t%s\n',['mlModel.setField("' n '",'],['new MLDouble(null, new double[] {' n '?1:0}, 1));' comment]);
		continue
	end
	% Matrix
	if hasstr(c,'Matrix')
		[n comment] = splitline(c,'Matrix');
		if ~isempty(comment)
			fprintf(['\t\t' comment '\n']);
		end
		fprintf('\t\t%-50s%-80s\n',['mlModel.setField("' n '",'],['new MLDouble(null, ' n '.getDouble()));']);
		continue
	end
	% double[][]
	if hasstr(c,'double[][] ')
		[n comment] = splitline(c,'double\[\]\[\]');
		if ~isempty(comment)
			fprintf(['\t\t' comment '\n']);
		end
		fprintf('\t\t%-50s%-80s\n',['mlModel.setField("' n '",'],['new MLDouble(null, ' n '));']);
		continue
	end
	% Arrays
	if hasstr(c,'ArrayList')
		% Extract class name
		sraw = regexp(c,'ArrayList<','split');
		sraw2 = regexp(sraw{2},'>','split');
		nClass = sraw2{1};
		% Extract object name (like before)
		[nObj comment] = splitline(c,'>');

		fprintf(['\n\t\t// ' nObj '\n']);
		fprintf(['\t\tN = ' nObj '.size();\n']);
		fprintf(['\t\tMLStructure ml' nObj ' = new MLStructure(null, new int[] {' nObj '.size() ,1});' comment '\n']);
		fprintf('\t\tfor(int ii=0; ii<N; ii++) {\n');
		fprintf(['\t\t\t' nClass ' obj = ' nObj '.get(ii);\n']);
		% Read fields
		fid2=fopen(['../src/cell/' nClass '.java']);
		c2 = '';
		while isempty(strfind(c2,'//////////////////////////////////'))
			c2=fgetl(fid2);
			% Ignore section
			if isempty(strtrim(c)) || ...
					~isempty(strfind(c2,'package')) || ...
					~isempty(strfind(c2,'import')) || ...
					~isempty(strfind(c2,'public class')) || ...
					~isempty(strfind(c2,'//////////////////////////////////'))
				continue
			end
			
			% Operate on the line
			% comment
			c2trim = strtrim(c2);
			if length(c2trim)>2 && strcmp(c2trim(1:2),'//') && ~strcmp(c2trim(1:3),'///')
% 				fprintf(['\t\t\t' c2trim '\n']);
				continue
			end
			% double
			if hasstr(c2,'double ')
				[n comment] = splitline(c2,'double');
				fprintf('\t\t\t%-50s%-80s\t%s\n',['ml' nObj '.setField("' n '",'],['new MLDouble(null, new double[] {obj.' n '}, 1), ii);'],comment);
				continue
			end
			% double[]
			if hasstr(c2,'double[]')
				[n comment] = splitline(c2,'double\[\]');
				fprintf('\t\t\t%-50s%-80s\t%s\n',['ml' nObj '.setField("' n '",'],['new MLDouble(null, obj.' n ', obj.' n '.length), ii);'],comment);
				continue
			end
			% int
			if hasstr(c2,'int ')
				[n comment] = splitline(c2,'int');
				fprintf('\t\t\t%-50s%-80s\t%s\n',['ml' nObj '.setField("' n '",'],['new MLDouble(null, new double[] {obj.' n '}, 1), ii);'], comment);
				continue
			end
			% String
			if hasstr(c2,'String ')
				[n comment] = splitline(c2,'String');
				fprintf('\t\t\t%-50s%-80s\t%s\n',['ml' nObj '.setField("' n '",'],['new MLChar(null, new String[] {obj.' n '}, obj.' n '.length()), ii);'], comment);
				continue
			end
			% String[]
			if hasstr(c2,'String[] ')
				[n comment] = splitline(c,'String\[\]');
				fprintf('\t\t\t%-50s%-80s\t%s\n',['ml' nObj '.setField("' n '",'],['new MLChar(null, n);'], comment);
				continue
			end
			% Vector3d
			if hasstr(c2,'Vector3d ')
				[n comment] = splitline(c2,'Vector3d');
				fprintf('\t\t\t%-50s%-80s\t%s\n',['ml' nObj '.setField("' n '",'],['new MLDouble(null, new double[] {obj.' n '.x, obj.' n '.y, obj.' n '.z}, 3), ii);'], comment);
				continue
			end
% 			% Vector3d[]		//FIXME
% 			if hasstr(c2,'Vector3d[] ')
% 				[n comment] = splitline(c2,'Vector3d\[\]');
% 				
% 				int NField = (int)(movementTimeStepEnd/movementTimeStep);			// -1 for not last index, +1 for initial position and velocity
% 			double[] posSave = new double[NSave*3];
% 			double[] velSave = new double[NSave*3];
% 			for(int ii=0; ii<NSave; ii++) {
% 				posSave[ii] 		= ball.posSave[ii].x; 	velSave[ii] 		= ball.velSave[ii].x;
% 				posSave[ii+NSave] 	= ball.posSave[ii].y; 	velSave[ii+NSave] 	= ball.velSave[ii].y;
% 				posSave[ii+2*NSave] = ball.posSave[ii].z; 	velSave[ii+2*NSave] = ball.velSave[ii].z;
% 			}
% 			
% 				fprintf('\t\t\t%-50s%-80s\t%s\n',['int NField = (int)(movementTimeStepEnd/movementTimeStep);'], comment);
% 				fprintf('\t\t\t%-50s%-80s\t%s\n',['for(int jj=0; jj<NField; jj++) {'], comment);
% 				fprintf('\t\t\t%-50s%-80s\t%s\n',['double'], comment);
% 				
% 				fprintf('\t\t\t%-50s%-80s\t%s\n',['ml' nObj '.setField("' n '",'],['new MLDouble(null, new double[] {obj.' n '.x, obj.' n '.y, obj.' n '.z}, 3), ii);'], comment);
% 				continue
% 			end
			% boolean
			if hasstr(c2,'boolean ')
				[n comment] = splitline(c2,'boolean');
				fprintf('\t\t\t%-50s%-80s\t%s\n',['ml' nObj '.setField("' n '",'],['new MLDouble(null, new double[] {obj.' n '?1:0}, 1), ii);'], comment);
				continue
			end
			% Arrays (this one is completely different from the one above!)
			if hasstr(c2,'Array')
				fprintf('\t\t\t\n');
				if hasstr(c2,'[]')		% If already array
					% Extract class name
					[nClass2 ~] = splitline(c2,'\[\]',1);
					% Extract object name (like before)
					[nObj2 comment2] = splitline(c2,'\[\]');
					fprintf(['\t\t\tarrayIndex = new double[obj.' nObj2 '.length];\n'])
					fprintf(['\t\t\tfor(int jj=0; jj<obj.' nObj2 '.length; jj++)\t']);
					fprintf(['arrayIndex[jj] = obj.' nObj2 '[jj].Index()+1;\n']);
				elseif hasstr(c2,'>')
					% Extract class name
					s2raw = regexp(c2,'ArrayList<','split');
					s2raw2 = regexp(s2raw{2},'>','split');
					nClass2 = s2raw2{1};
					% Extract object name (like before)
					[nObj2 comment2] = splitline(c2,'>');
					fprintf(['\t\t\tarrayIndex = new double[obj.' nObj2 '.size()];\n'])
					fprintf(['\t\t\tfor(int jj=0; jj<obj.' nObj2 '.size(); jj++)\t']);
					fprintf(['arrayIndex[jj] = obj.' nObj2 '.get(jj).Index()+1;\n']);
				else
					throw(['Cannot recognise type of array in: ' c2])
				end
				fprintf('\t\t\t%-50s%-80s\t%s\n',['ml' nObj '.setField("' nObj2 '",'],['new MLDouble(null, arrayIndex, 1), ii);'],comment2);
				continue
			end
			
			% And if no match was found, just ignore (could be reference to another array nested in CModel, etc)
		end
		fprintf('\t\t}\n');
		fprintf(['\t\tmlModel.setField("' nObj '", ml' nObj ');\n']);
		continue
	end
	
	% Are you still here?
	if hasstr(c,'public')		% Otherwise, just skip it. It's a continued line
		error(['Do not know what to do with line:' c]);
	end
end

fclose(fid);

% Wrap it up
fprintf('\n\t\t// Create a list and add mlModel\n');
fprintf('\t\tArrayList<MLArray> list = new ArrayList<MLArray>(1);\n')
fprintf('\t\tlist.add(mlModel);\n');
fprintf('\t\ttry {\n');
fprintf('\t\t\tnew MatFileWriter(name + "/output/" + String.format("g%%04dm%%04d", growthIter, movementIter) + ".mat",list);\n');
fprintf('\t\t} catch (IOException e) {\n');
fprintf('\t\t\te.printStackTrace();\n');
fprintf('\t\t}\n');
fprintf('\t}\n');

pause; pause

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function bool = hasstr(s,pat)
if ~isempty(strfind(s,pat))
	bool = true;
else
	bool = false;
end

function s = cleanup(s)
% Clean ;
s(strfind(s,';'))=[];

% Remove part after =
sraw = regexp(s,'=','split');
s = sraw{1};

% Remove brackets []
s(strfind(s,'\['))=[];
s(strfind(s,'\]'))=[];

% clean spaces
s = strtrim(s);


function [name, comment] = splitline(varargin)

s = varargin{1};
pat = varargin{2};
if length(varargin)==3
	split = varargin{3};
elseif length(varargin)==2
	split = 2;
else
	throw('Unknown arguments for splitline');
end

sraw = regexp(s,pat,'split');			% cut at pat (e.g. 'double')
sraw2 = regexp(sraw{split},'//','split');	% cut at comment ('//')

sraw2{1}(strfind(sraw2{1},'public'):strfind(sraw2{1},'public')+6) = [];	% remove 'public'

name = cleanup(sraw2{1});

comment = '';
if length(sraw2) > 1
	comment = ['// ' strtrim(sraw2{2})];
end
