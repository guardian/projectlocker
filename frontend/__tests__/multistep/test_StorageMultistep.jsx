import React from 'react';
import {shallow,mount} from 'enzyme';
import moxios from 'moxios';
import StorageMultistep from '../../app/multistep/StorageMultistep.jsx';
import sinon from 'sinon';

describe("StorageMultistep", ()=>{
    beforeEach(() => moxios.install());
    afterEach(() => moxios.uninstall());

    it("should download data from the known storage types on mount", (done)=>{
        const rendered=shallow(<StorageMultistep/>);

        return moxios.wait(()=>{
            expect(moxios.requests.mostRecent().config.url).toEqual('/api/storage/knowntypes');
            let request = moxios.requests.mostRecent();
            request.respondWith({
                status: 200,
                response: {status: 'ok',types: ['test one','test two']}
            }).then(() => {
                expect(rendered.instance().state.strgTypes).toEqual(['test one','test two']);
                done();
            }).catch(error=>{
                console.error(error);
                done(error);
            })
        });
    });


});