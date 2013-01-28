function CreateSave

fid=fopen('../src/cell/CModel.java','r');
fid2=fopen('../src/ser2mat/ser2mat.java','w');
c = '';

% Make header
fprintf(fid2,'package ser2mat;\n\n');

fprintf(fid2,'import java.io.IOException;\n');
fprintf(fid2,'import java.util.ArrayList;\n\n');
fprintf(fid2,'import cell.*;\n');
fprintf(fid2,'import jmatio.*;\n\n');
fprintf(fid2,'public class ser2mat {\n');

fprintf(fid2,'\tpublic static void Convert(CModel model) {\n');
fprintf(fid2,'\t\tMLStructure mlModel = new MLStructure("model", new int[] {1,1});\n');
fprintf(fid2,'\t\tint N;\n');		% we'll need this one for the arrays
fprintf(fid2,'\t\tdouble[] arrayIndex;\n');	% we'll need this one for the arrays' arrays
		

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
		fprintf(fid2,['\t\t' ctrim '\n']);
		continue
	end
	% double
	if hasstr(c,'double ')
		[n comment] = splitline(c,'double');
		fprintf(fid2,'\t\t%-50s%-80s\t%s\n',['mlModel.setField("' n '",'],['new MLDouble(null, new double[] {model.' n '}, 1));'],comment);
		continue
	end
	% double[]
	if hasstr(c,'double[] ')
		[n comment] = splitline(c,'double\[\]');
		fprintf(fid2,'\t\t%-50s%-80s\t%s\n',['mlModel.setField("' n '",'],['new MLDouble(null, model.' n ', model.' n '.length));'], comment);
		continue
	end
	% int
	if hasstr(c,'int ')
		[n comment] = splitline(c,'int');
		fprintf(fid2,'\t\t%-50s%-80s\t%s\n',['mlModel.setField("' n '",'],['new MLDouble(null, new double[] {model.' n '}, 1));'],comment);
		continue
	end
	% int[]					// Ugly one as we can't cast int[] --> double[]
	if hasstr(c,'int[] ')
		[n comment] = splitline(c,'int\[\]');
		fprintf(fid2,'\t\t//\n');
		fprintf(fid2,'\t\tdouble[] D%s = new double[model.%s.length];',n,n);
		fprintf(fid2,'\t\tfor(int ii=0; ii<model.%s.length; ii++)\t\tD%s[ii] = model.%s[ii];',n,n,n);
		fprintf(fid2,'\t\t%-50s%-80s\t%s\n',['mlModel.setField("' n '",'],['new MLDouble(null, D' n ', model.' n '.length));'], comment);
		fprintf(fid2,'\t\t//\n');
		continue
	end
	% Vector3d
	if hasstr(c,'Vector3d ')
		[n comment] = splitline(c,'Vector3d');
		fprintf(fid2,'\t\t%-50s%-80s\t%s\n',['mlModel.setField("' n '",'],['new MLDouble(null, new double[] {model.' n '.x, model.' n '.y, model.' n '.z}, 3));'], comment);
		continue
	end
	% Vector3d[]
	if hasstr(c,'Vector3d[] ')
		% Ignore (TODO)
		continue
	end
	% String
	if hasstr(c,'String ')
		[n comment] = splitline(c,'String');
		fprintf(fid2,'\t\t%-50s%-80s\t%s\n',['mlModel.setField("' n '",'],['new MLChar(null, new String[] {model.' n '}, model.' n '.length()));'], comment);
		continue
	end
	% String[]
	if hasstr(c,'String[] ')
		[n comment] = splitline(c,'String\[\]');
		fprintf(fid2,'\t\t%-50s%-80s\t%s\n',['mlModel.setField("' n '",'],['new MLChar(null, model.' n '));'], comment);
		continue
	end
	% boolean
	if hasstr(c,'boolean ')
		[n comment] = splitline(c,'boolean');
		fprintf(fid2,'\t\t%-50s%-80s\t%s\n',['mlModel.setField("' n '",'],['new MLDouble(null, new double[] {model.' n '?1:0}, 1));'], comment);
		continue
	end
	% Matrix
	if hasstr(c,'Matrix')
		[n comment] = splitline(c,'Matrix');
		if ~isempty(comment)
			fprintf(fid2,['\t\t' comment '\n']);
		end
		fprintf(fid2,'\t\t%-50s%-80s\n',['mlModel.setField("' n '",'],['new MLDouble(null, model.' n '.getDouble()));']);
		continue
	end
	% double[][]
	if hasstr(c,'double[][] ')
		[n comment] = splitline(c,'double\[\]\[\]');
		if ~isempty(comment)
			fprintf(fid2,['\t\t' comment '\n']);
		end
		fprintf(fid2,'\t\t%-50s%-80s\n',['mlModel.setField("' n '",'],['new MLDouble(null, model.' n '));']);
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

		fprintf(fid2,['\n\t\t// ' nObj '\n']);
		fprintf(fid2,['\t\tN = model.' nObj '.size();\n']);
		fprintf(fid2,['\t\tMLStructure ml' nObj ' = new MLStructure(null, new int[] {model.' nObj '.size() ,1});' comment '\n']);
		fprintf(fid2,'\t\tfor(int ii=0; ii<N; ii++) {\n');
		fprintf(fid2,['\t\t\t' nClass ' obj = model.' nObj '.get(ii);\n']);
		% Read fields
		fid3=fopen(['../src/cell/' nClass '.java']);
		c2 = '';
		while isempty(strfind(c2,'//////////////////////////////////'))
			c2=fgetl(fid3);
			% Ignore section
			if isempty(strtrim(c)) || ...
					~isempty(strfind(c2,'package')) || ...
					~isempty(strfind(c2,'import')) || ...
					~isempty(strfind(c2,'public class')) || ...
					~isempty(strfind(c2,'//////////////////////////////////'))
				continue
			end
			
			% Operate on the line
			% comment2
			c2trim = strtrim(c2);
			if length(c2trim)>2 && strcmp(c2trim(1:2),'//') && ~strcmp(c2trim(1:3),'///')
% 				fprintf(fid2,['\t\t\t' c2trim '\n']);
				continue
			end
			% double
			if hasstr(c2,'double ')
				[n comment] = splitline(c2,'double');
				fprintf(fid2,'\t\t\t%-50s%-80s\t%s\n',['ml' nObj '.setField("' n '",'],['new MLDouble(null, new double[] {obj.' n '}, 1), ii);'],comment);
				continue
			end
			% double[]
			if hasstr(c2,'double[]')
				[n comment] = splitline(c2,'double\[\]');
				fprintf(fid2,'\t\t\t%-50s%-80s\t%s\n',['ml' nObj '.setField("' n '",'],['new MLDouble(null, obj.' n ', obj.' n '.length), ii);'],comment);
				continue
			end
			% int
			if hasstr(c2,'int ')
				[n comment] = splitline(c2,'int');
				fprintf(fid2,'\t\t\t%-50s%-80s\t%s\n',['ml' nObj '.setField("' n '",'],['new MLDouble(null, new double[] {obj.' n '}, 1), ii);'], comment);
				continue
			end
			% String
			if hasstr(c2,'String ')
				[n comment] = splitline(c2,'String');
				fprintf(fid2,'\t\t\t%-50s%-80s\t%s\n',['ml' nObj '.setField("' n '",'],['new MLChar(null, new String[] {obj.' n '}, obj.' n '.length()), ii);'], comment);
				continue
			end
			% String[]
			if hasstr(c2,'String[] ')
				[n comment] = splitline(c,'String\[\]');
				fprintf(fid2,'\t\t\t%-50s%-80s\t%s\n',['ml' nObj '.setField("' n '",'],['new MLChar(null, obj.' n ');'], comment);
				continue
			end
			% Vector3d
			if hasstr(c2,'Vector3d ')
				[n comment] = splitline(c2,'Vector3d');
				fprintf(fid2,'\t\t\t%-50s%-80s\t%s\n',['ml' nObj '.setField("' n '",'],['new MLDouble(null, new double[] {obj.' n '.x, obj.' n '.y, obj.' n '.z}, 3), ii);'], comment);
				continue
			end
			% Vector3d[]
			if hasstr(c2,'Vector3d[] ')
				[n comment] = splitline(c2,'Vector3d\[\]');
				fprintf(fid2,['\t\t\t//' n '\n']);
				if ~isempty(comment)
					fprintf(fid2,['\t\t' comment '\n']);
				end
				fprintf(fid2,['\t\t\t{int N2 = (int) obj.' n '.length;\n']);
				fprintf(fid2,['\t\t\tdouble[][] ' n ' = new double[N2][3];\n']);
				fprintf(fid2,['\t\t\tfor(int jj=0; jj<N2; jj++) {\n']);
				fprintf(fid2,['\t\t\t\t' n '[jj][0] = obj.' n '[jj].x;\n']);
				fprintf(fid2,['\t\t\t\t' n '[jj][1] = obj.' n '[jj].y;\n']);
				fprintf(fid2,['\t\t\t\t' n '[jj][2] = obj.' n '[jj].z;\n']);
				fprintf(fid2,['\t\t\t}\n']);
				fprintf(fid2,'\t\t\t%-50s%-80s\n',['ml' nObj '.setField("' n '",'],['new MLDouble(null, ' n '));}']);
% 				fprintf(fid2,'\t\t\t%-50s%-80s\t%s\n',['ml' nObj '.setField("' n '",'],['new MLDouble(null, obj.' n ', obj.' n '.length), ii);'],comment);
% 				fprintf(fid2,'\t\t%-50s%-80s\n',['mlModel.setField("' n '",'],['new MLDouble(null, model.' n '));']);
				continue
			end
			% boolean
			if hasstr(c2,'boolean ')
				[n comment] = splitline(c2,'boolean');
				fprintf(fid2,'\t\t\t%-50s%-80s\t%s\n',['ml' nObj '.setField("' n '",'],['new MLDouble(null, new double[] {obj.' n '?1:0}, 1), ii);'], comment);
				continue
			end
			% Arrays (this one is completely different from the one above!)
			if hasstr(c2,'Array')
				fprintf(fid2,'\t\t\t\n');
				if hasstr(c2,'[]')		% If already array
					% Extract class name
					[nClass2 ~] = splitline(c2,'\[\]',1);
					% Extract object name (like before)
					[nObj2 comment2] = splitline(c2,'\[\]');
					fprintf(fid2,['\t\t\tarrayIndex = new double[obj.' nObj2 '.length];\n']);
					fprintf(fid2,['\t\t\tfor(int jj=0; jj<obj.' nObj2 '.length; jj++)\t']);
					fprintf(fid2,['arrayIndex[jj] = obj.' nObj2 '[jj].Index();\n']);
				elseif hasstr(c2,'>')
					% Extract class name
					s2raw = regexp(c2,'ArrayList<','split');
					s2raw2 = regexp(s2raw{2},'>','split');
					nClass2 = s2raw2{1};
					% Extract object name (like before)
					[nObj2 comment2] = splitline(c2,'>');
					fprintf(fid2,['\t\t\tarrayIndex = new double[obj.' nObj2 '.size()];\n']);
					fprintf(fid2,['\t\t\tfor(int jj=0; jj<obj.' nObj2 '.size(); jj++)\t']);
					fprintf(fid2,['arrayIndex[jj] = obj.' nObj2 '.get(jj).Index();\n']);
				else
					throw(['Cannot recognise type of array in: ' c2])
				end
				fprintf(fid2,'\t\t\t%-50s%-80s\t%s\n',['ml' nObj '.setField("' nObj2 '",'],['new MLDouble(null, arrayIndex, 1), ii);'],comment2);
				continue
			end
			
			% And if no match was found, just ignore (could be reference to another array nested in CModel, etc)
		end
		fprintf(fid2,'\t\t}\n');
		fprintf(fid2,['\t\tmlModel.setField("' nObj '", ml' nObj ');\n']);
		continue
	end
	
	% Are you still here?
	if hasstr(c,'public')		% Otherwise, just skip it. It's a continued line
		error(['Do not know what to do with line:' c]);
	end
end

fclose(fid);

% Wrap it up
fprintf(fid2,'\n\t\t// Create a list and add mlModel\n');
fprintf(fid2,'\t\tArrayList<MLArray> list = new ArrayList<MLArray>(1);\n');
fprintf(fid2,'\t\tlist.add(mlModel);\n');
fprintf(fid2,'\t\ttry {\n');
fprintf(fid2,'\t\t\tnew MatFileWriter(model.name + "/output/" + String.format("g%%04dr%%04d", model.growthIter, model.relaxationIter) + ".mat",list);\n');
fprintf(fid2,'\t\t} catch (IOException e) {\n');
fprintf(fid2,'\t\t\te.printStackTrace();\n');
fprintf(fid2,'\t\t}\n');
fprintf(fid2,'\t}\n');
fprintf(fid2,'}\n');

fclose(fid2);

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
