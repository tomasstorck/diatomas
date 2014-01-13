function Compare(A,B)

todo = fieldnames(A);
while ~isempty(todo)
    fn = todo{1};
    try
        fA = eval(['A.' fn]);
        fB = eval(['B.' fn]);
        if ~isstruct(fA)
            for ii=1:max(length(fA),length(fB))
                same = eval(['A.' fn '==B.' fn]);
                if ~same
                    disp(fn);
                end
            end
        else
            fns2A = fieldnames(eval(['A.' fn]));
            fns2B = fieldnames(eval(['B.' fn]));
            for ii=1:max(length(fns2A),length(fns2B))
                fn2 = [fns2A{ii}];
                for jj=1:max(length(fA),length(fB))
                    todo{end+1} = [fn '(' num2str(jj) ').' fn2];
                end
            end
        end
    catch
        disp(['cannot eval: ' fn]);
    end
    
    % kick out what we just analysed
    todo(1) = [];
end