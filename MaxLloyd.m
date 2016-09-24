%
% Version: 1.0
% Author: Keith Henderson
% Contact: keith@llnl.gov
%

%
% This function computes the Max-Lloyd quantization of a matrix.
% Arguments:
% A is the matrix of values to be quantized.
% L is the number of quanta.
% thresh is the threshold for iteration. When the difference in error terms
%  between two steps is below thresh, the algorithm exits.
%

function [Q, M] = MaxLloyd(A, L, thresh)

flat = A(:);
Q = zeros(size(flat));
M = zeros(size(flat));

n = size(flat);


mn = min(flat);
mx = max(flat);
L = ceil(L);
Y = zeros(1,L);

for i=1:L
    Y(i) = (i-1)*(mx-mn)/L;
end   

oldErr = sum(flat.*flat);

while 1
    ySize = zeros(1,L);
    newY = zeros(1,L);
    newErr = 0;
    for i=1:n
        bestJ = 1;
        bestDiff = abs(flat(i) - Y(1));
        for j = 2:L
            if abs(flat(i) - Y(j)) < bestDiff
               bestJ = j;
               bestDiff = abs(flat(i) - Y(j));
            end
        end
        M(i) = bestJ;
        ySize(bestJ) = ySize(bestJ) + 1;
        newY(bestJ) = newY(bestJ) + flat(i);
        
        
    end
    for i=1:L
        Y(i) = newY(i)/max(ySize(i),1);
    end
    for i=1:n
        Q(i) = Y(M(i));
    end;
    
    newErr = newErr + (flat(i)-Q(i))^2;
    if oldErr - newErr < thresh
        break
    end
    oldErr = newErr;
    
    
end

j = 1;
mp = zeros(1,L);
for i=1:n
    if mp(M(i)) == 0
        mp(M(i)) = j;
        j = j + 1;
    end
    M(i) = mp(M(i));
end

Q = reshape(Q, size(A));
M = reshape(M, size(A));

end


