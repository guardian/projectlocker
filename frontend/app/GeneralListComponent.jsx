import React from 'react';
import axios from 'axios';

class GeneralListComponent extends React.Component {
    constructor(props){
        super(props);
        this.state = {
            data: [],
            hovered: false
        };
        this.endpoint='/unknown'
    }

    componentDidMount() {
        this.reload();
    }

    reload(){
        let component = this;

        axios.get(this.endpoint).then(function(result){
            component.setState({
                data: result.data.result
            });
        }).catch(function (error) {
            console.error(error);
        });
    }

    render() {

    }
}

export default GeneralListComponent;