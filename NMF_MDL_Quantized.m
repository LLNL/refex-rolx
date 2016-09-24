%
% Version: 1.0
% Author: Keith Henderson
% Contact: keith@llnl.gov
%

%
% This function tries a number of different model sizes using NMF_LS_new()
% and selects the model that minimizes description length. Quantization is 
% performed using Max-Lloyd and values are compressed with Huffman Codes.
%
% Inputs:
%  V - (node x feature) matrix
%  bins - [optional] number of quantization bins (default=log2(n))
%  maxRoles - [optional] maximum number of roles to try
%
% Outputs:
%  F - (role x feature) matrix
%  G - (node x role) matrix
%  dLen - description length of model (bits)
%
function [F, G, dLen] = NMF_MDL_Quantized(V,bins,maxRoles)
more off;
format short;

v = V(:);

mx = max(v);
[n,d] = size(V);
if nargin < 2
    bins = log2(n);
end
if nargin < 3
    maxRoles = n;
end
maxRoles = min(maxRoles, n);
maxRoles = min(maxRoles, d);

G0 = rand(n,maxRoles)*mx;
F0 = rand(d,maxRoles)*mx;


minDLen = 1e20;


bits = log2(bins);
thresh = 1e-5;

numWorse = 0;
for numRoles = 1:maxRoles
    [F,G] = NMF_LS_new(V,1000,numRoles, 0, F0(:,1:numRoles), ...
        G0(:,1:numRoles));
    
    [Ft,Fi] = MaxLloyd(F, bins, thresh);
    [Gt,Gi] = MaxLloyd(G, bins, thresh);
    
    cost = HuffmanCost(Fi, bits, 0) + HuffmanCost(Gi, bits, 0);


    E = abs(V-Gt*Ft);

    err = E(:);
    
    logLikelihood = -0.5*log2(exp(1))/(var(v))*sum(err.^2);
    dLen = cost - logLikelihood;
    fprintf(1, ...
        'numRoles = %d, cost = %1.0f, -logLikelihood = %1.0f, dLen = %1.0f\n',...
        numRoles, cost, -logLikelihood, dLen);
    if dLen < minDLen
       minDLen = dLen;
       minF = F;
       minG = G;
       numWorse = 0;
    else
        numWorse = numWorse + 1;
        if numWorse == 5 
            break 
        end
    end
end

F = minF;
G = minG;
dLen = minDLen;
