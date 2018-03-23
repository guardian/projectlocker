import React from 'react';
import moxios from 'moxios';
import {shallow, mount} from 'enzyme';
import FileEntryFilterComponent from '../../app/filter/FileEntryFilterComponent.jsx';
import sinon from 'sinon';

describe("FileEntryFilterComponent", ()=>{
    beforeEach(()=>moxios.install());
    afterEach(()=>moxios.uninstall());

    it("Should load in valid owners on mount", (done)=>{
        const updateCb = sinon.spy();

        const rendered = shallow(<FileEntryFilterComponent filterDidUpdate={updateCb} filterTerms={{}}/>);

        return moxios.wait(()=>{
            const request = moxios.requests.mostRecent();
            try {
                expect(request.config.url).toEqual('/api/file/distinctowners');
            } catch(e) {
                done.fail(e);
            }

            request.respondWith({
                status: 200,
                response: {'status': 'ok','result':['Rob','Kath','Jen']}
            }).then(() => {
                expect(rendered.instance().state.distinctOwners).toEqual(['Rob','Kath','Jen']);
                done();
            }).catch(error=>{
                console.error(error);
                done.fail(error);
            })
        })
    })
});