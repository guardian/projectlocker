import React from 'react';
import {shallow,mount} from 'enzyme';
import moxios from 'moxios';
import PostrunMultistep from '../../app/multistep/PostrunMultistep.jsx';
import sinon from 'sinon';

describe("PostrunMultistep", ()=>{
    beforeEach(()=>moxios.install());
    afterEach(()=>moxios.uninstall());

    it("should download data on mount", (done)=>{
        const rendered = shallow(<PostrunMultistep match={{params: {itemid: 3}}}/>);

        return moxios.wait(()=>{
            const rq = moxios.requests.at(0);
            try {
                expect(rq.url).toEqual('/api/postrun/3');
                expect(moxios.requests.at(1).url).toEqual('/api/postrun/3/source');
            }catch(err){
                done.fail(err);
            }
            rq.respondWith({
                status: 200,
                response: {"status":"ok","result":{"id":1,"runnable":"args_test_4.py","title":"args_test_4.py","owner":"system","version":1,"ctime":"2018-03-07T08:13:34.000+0000"}}
            }).then(()=>{
                done();
            }).catch(error=>done.fail(error));
        })
    })
});