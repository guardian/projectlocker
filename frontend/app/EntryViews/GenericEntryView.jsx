import React from 'react';
import axios from 'axios';
import PropTypes from 'prop-types';

class GenericEntryView extends React.Component {
    static propTypes = {
        entryId: PropTypes.number.isRequired
    };

    constructor(props){
        super(props);

        this.endpoint = "/unknown";

        this.state = {
            loading: false,
            content: {}
        }
    }

    loadData() {
        this.setState({loading: true}, ()=>{
            axios.get(this.endpoint + "/" + this.props.entryId)
                .then(response=>{this.setState({content: response.data.result, loading: false}); console.log("entry Id is " + this.props.entryId);})
                .catch(error=>this.setState({lastError: error, loading: false}))
        })
    }

    componentWillMount(){
        if(this.props.entryId) this.loadData();
    }

    componentDidUpdate(oldProps, oldState){
        if(oldProps.entryId!==this.props.entryId) this.loadData();
    }

    render(){
        return <span>{this.state.content.name}</span>
    }
}

export default GenericEntryView;
